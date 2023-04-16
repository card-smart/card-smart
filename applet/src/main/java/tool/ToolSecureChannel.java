package tool;

import javacard.security.AESKey;
import javacard.security.KeyBuilder;

import javax.crypto.*;
import javax.smartcardio.CommandAPDU;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;

public class ToolSecureChannel {
    KeyPairGenerator keyGen;
    private final javacardx.crypto.Cipher aesCbc;
    private final javacard.security.Signature scMac;
    private final AESKey aesKey;
    private final AESKey macKey;
    byte[] iv;
    private KeyPair keyPair;
    SecureRandom random;

    public ToolSecureChannel() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        keyGen = KeyPairGenerator.getInstance("EC");
        random = new SecureRandom();
        // prepare internal crypto representations
        this.aesCbc = javacardx.crypto.Cipher.getInstance(javacardx.crypto.Cipher.ALG_AES_CBC_ISO9797_M2, false);
        this.scMac = javacard.security.Signature.getInstance(javacard.security.Signature.ALG_AES_MAC_128_NOPAD, false);
        this.iv = new byte[16];
        this.macKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_256, false);
        this.aesKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_256, false);
        // generate fresh keypair
        this.generateECKeyPair();
    }

    /**
     * Prepare payload for initialization APDU command
     * @param cardPublicKeyBytes uncompressed public key point from card
     * @param PIN padded init PIN to be set on card
     * @param pairingSecret pairing secret to be set on card
     * @return payload
     */
    public byte[] prepareInitializationPayload(byte[] cardPublicKeyBytes, byte[] PIN, byte[] pairingSecret) {
        // generate IV for encryption
        this.generateIV(iv);
        // derive simple encryption key and set it as key
        ECPublicKey cardPublicKey = this.convertBytesToPublicKey(cardPublicKeyBytes);
        byte[] simpleDerivedSecret = null;
        try {
            simpleDerivedSecret = this.getDerivedSecret(cardPublicKey, (ECPrivateKey) keyPair.getPrivate());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            System.out.println(e);
            return null;
        }
        aesKey.setKey(simpleDerivedSecret, (short) 0);

        // move [PIN | pairingSecret] into buffer
        byte[] data = new byte[42];
        System.arraycopy(PIN, 0, data, 0, PIN.length);
        System.arraycopy(pairingSecret, 0, data, PIN.length, pairingSecret.length);
        // encrypt with previously set key
        byte[] encrypted = this.aesEncrypt(data);

        // init command APDU payload: publicKey [65 B] | IV [16 B] | encrypted [48 B]
        byte[] payload = new byte[129];
        byte[] publicKeyBytes = this.getPublicKeyBytes((ECPublicKey) keyPair.getPublic());
        // copy into APDU data part
        System.arraycopy(publicKeyBytes, 0, payload, 0, publicKeyBytes.length);
        System.arraycopy(iv, 0, payload, publicKeyBytes.length, iv.length);
        System.arraycopy(encrypted, 0, payload, publicKeyBytes.length + iv.length, encrypted.length);

        // remove auxiliary encryption key set from derived secret
        aesKey.clearKey();

        return payload;
    }

    /**
     * Generate fresh EC keypair and return its public key in uncompressed from
     * @return 0x04 | x coordinate [32 B] | y coordinate [32 B]
     */
    public byte[] getFreshPublicKeyBytes() {
        try {
            this.generateECKeyPair();
        } catch (InvalidAlgorithmParameterException e) {
            System.out.println(e);
            return null;
        }
        return this.getPublicKeyBytes((ECPublicKey) keyPair.getPublic());
    }

    public void createSharedSecrets(byte[] pairingSecret, byte[] cardPublicKeyBytes, byte[] responseData) {
        // convert uncompressed point returned from card into public key object
        ECPublicKey cardPublicKey = this.convertBytesToPublicKey(cardPublicKeyBytes);
        // parse returned salt and IV
        byte[] salt = new byte[32];
        System.arraycopy(responseData, 0, salt, 0, salt.length);
        System.arraycopy(responseData, salt.length, iv, 0, iv.length);
        // encryption and MAC keys
        byte[] sharedSecrets = null;
        try {
            sharedSecrets = this.computeSharedSecrets(cardPublicKey, (ECPrivateKey) keyPair.getPrivate(), pairingSecret, salt);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            System.out.println(e);
            return;
        }
        aesKey.setKey(sharedSecrets, (byte) 0);
        macKey.setKey(sharedSecrets, (byte) 32);
    }

    /**
     * Prepare APDU with encrypted payload and appended MAC tag
     * @param CLA CLA byte
     * @param INS INS byte
     * @param data data to be sent
     * @return prepared CommandAPDU object
     */
    // TODO change to match decomposition of code - is it good to return CommandAPDU?
    public CommandAPDU prepareSecureAPDU(byte CLA, byte INS, byte[] data) {
        byte[] apduBuffer;
        short filledLength = 0;
        if (data != null && data.length > 0) {
            // no data part in APDU
            byte[] encryptedPayload = this.aesEncrypt(data);
            apduBuffer = new byte[5 + encryptedPayload.length + 16];
            apduBuffer[4] = (byte) (encryptedPayload.length + 16); // payload + MAC size
            System.arraycopy(encryptedPayload, 0, apduBuffer, 5, encryptedPayload.length);
            filledLength += encryptedPayload.length;
        } else {
            apduBuffer = new byte[5 + 16];
            apduBuffer[4] = (byte) 16;
        }

        // prepare temporary buffer with prepended instructions
        apduBuffer[0] = CLA;
        apduBuffer[1] = INS;
        apduBuffer[2] = apduBuffer[3] = 0; // P1, P2
        filledLength += 5;
        // compute MAC and append after payload in apduBuffer
        this.computeMacAndAppend(apduBuffer, filledLength);
        // set iv for next decryption
        this.setIV(iv, apduBuffer, (short) (apduBuffer.length - 16));
        // send to card
        return new CommandAPDU(apduBuffer);
    }

    /**
     * Get byte array of response data: payload | SW1 | SW2
     * @param responseData encrypted data buffer
     * @return byte response
     */
    public byte[] getResponseData(byte[] responseData) {
        // verify MAC tag
        boolean verified = this.verifyResponseMAC(responseData);
        if (!verified) {
            System.out.println("MAC not verified!");
            return null;
        }
        
        System.out.println("MAC verified!");
        // decrypt payload
        byte[] decrypted = this.aesDecrypt(responseData);
        if (decrypted.length > 0) {
            System.out.println("Decrypted payload!");
        }
        // set MAC as new iv for next encryption
        this.setIV(this.iv, responseData, (short) (responseData.length - 16));
        return decrypted;
    }

    /**
     * Set secret read from file supplied by input arguments
     * @param path to file with secret
     * @return newly generated pairing secret
     */
    public static byte[] createPairingSecret(String path) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        try {
            FileOutputStream out = new FileOutputStream(path);
            out.write(bytes);
            out.close();
        } catch (IOException e) {
            System.out.println("The pairing secret could not be established");
            return null;
        }
        return bytes;
    }

    /**
     * Convert ECPublicKey object into byte array representing uncompressed EC point
     * @param publicKey public key for conversion
     * @return public key as uncompressed bytes
     */
    private byte[] getPublicKeyBytes(ECPublicKey publicKey) {
        ECPoint publicKeyPoint = publicKey.getW();
        BigInteger x = publicKeyPoint.getAffineX();
        BigInteger y = publicKeyPoint.getAffineY();

        byte[] publicKeyBytes = new byte[65];
        publicKeyBytes[0] = 0x04; // prefix for uncompressed point
        byte[] xBytes = x.toByteArray();
        byte[] yBytes = y.toByteArray();
        int offset = xBytes.length > 32 ? 1 : 0; // it is possible, that the coordinate has 33 bytes sometimes
        System.arraycopy(xBytes, offset, publicKeyBytes, 1, xBytes.length - offset);
        offset = yBytes.length > 32 ? 1 : 0;
        System.arraycopy(yBytes, offset, publicKeyBytes, 33, yBytes.length - offset);
        return publicKeyBytes;
    }

    /**
     * Convert uncompressed EC point into public key object
     * @param ecPoint 65 bytes of uncompressed public key point
     * @return converted public key object
     * @apiNote <a href="https://stackoverflow.com/questions/26159149/how-can-i-get-a-publickey-object-from-ec-public-key-bytes">...</a>
     */
    private ECPublicKey convertBytesToPublicKey(byte[] ecPoint) {
        if(ecPoint[0] != '\04')
            throw new IllegalArgumentException();
        // split byte array into two coordinates
        byte[] xBytes = Arrays.copyOfRange(ecPoint, 1, 33);
        byte[] yBytes = Arrays.copyOfRange(ecPoint, 33, 65);
        BigInteger x = new BigInteger(1, xBytes);
        BigInteger y = new BigInteger(1, yBytes);

        // create EC point for public key
        ECPoint pubPoint = new ECPoint(x, y);

        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC", "SunEC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
            ECPublicKeySpec pubSpec = new ECPublicKeySpec(pubPoint, ecParameters);
            KeyFactory kf = KeyFactory.getInstance("EC");
            return (ECPublicKey)kf.generatePublic(pubSpec);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    /**
     * Generate tool ephemeral keypair over secp256r1 curve
     */
    private void generateECKeyPair() throws InvalidAlgorithmParameterException {
        this.keyGen.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        this.keyPair = keyGen.generateKeyPair();
    }

    /**
     * Derive 32B secret value using ECDH
     * @param cardPublicKey public key received from card converted into ECPublicKey
     * @param toolPrivateKey ephemeral private key of tool
     * @return 32B derived secret value
     */
    private  byte[] getDerivedSecret(ECPublicKey cardPublicKey, ECPrivateKey toolPrivateKey)
            throws NoSuchAlgorithmException, InvalidKeyException {
        KeyAgreement ecdh = KeyAgreement.getInstance("ECDH");
        ecdh.init(toolPrivateKey);
        ecdh.doPhase(cardPublicKey, true);
        return ecdh.generateSecret();
    }

    /**
     * Compute encryption and MAC key in same way as card does
     * @param cardPublicKey public key received from card
     * @param privateKey tool's private key
     * @param pairingSecret secret established by init
     * @param salt salt from opening of the secure channel
     * @return 64 B containing chained encryption and MAC key
     */
    private byte[] computeSharedSecrets(ECPublicKey cardPublicKey, ECPrivateKey privateKey, byte[] pairingSecret, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] derivedSecret = getDerivedSecret(cardPublicKey, privateKey);
        MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
        sha512.update(derivedSecret);
        sha512.update(pairingSecret);
        sha512.update(salt);
        return sha512.digest();
    }

    /**
     * Generate 16B IV and store it
     */
    private void generateIV(byte[] iv) {
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
    }

    /**
     * Set new IV for encryption, decryption and MAC
     * @param iv old IV
     * @param newIv new IV
     * @param newIvOffset offset of the new IV
     */
    private void setIV(byte[] iv, byte[] newIv, short newIvOffset) {
        System.arraycopy(newIv, newIvOffset, iv, 0, iv.length);
    }

    /**
     * Perform AES encryption on given data
     * @param data data to encrypt, not padded
     * @return encrypted array of bytes
     */
    private byte[] aesEncrypt(byte[] data) {
        int encryptedLength = data.length % 16 == 0 ? data.length / 16 : data.length / 16 + 1;
        encryptedLength *= 16;
        this.aesCbc.init(aesKey, javacardx.crypto.Cipher.MODE_ENCRYPT, this.iv, (short) 0, (short) 16);
        byte[] encryptedBuffer = new byte[encryptedLength];
        aesCbc.doFinal(data, (short) 0, (short) data.length, encryptedBuffer, (byte) 0);
        return encryptedBuffer;
    }

    /**
     * Compute MAC tag from 5 byte of APDU (instructions), 11 bytes from aux buffer and encrypted payload
     * @param apduBuffer prepared buffer with instructions and encrypted payload
     * @param length count of bytes in buffer
     */
    private void computeMacAndAppend(byte[] apduBuffer, short length)  {
        scMac.init(this.macKey, javacard.security.Signature.MODE_SIGN);
        scMac.update(apduBuffer, (short) 0, (short) 5); // first 5 instruction bytes
        scMac.update(this.iv, (short) 0, (short) 11); // pad them with some values
        scMac.sign(apduBuffer, (short) 5, (short) (length - 5), apduBuffer, length);
    }

    /**
     * Verify MAC over payload in response buffer
     * @param responseBuffer buffer with payload
     * @return true if verified, false otherwise
     */
    private boolean verifyResponseMAC(byte[] responseBuffer) {
        scMac.init(this.macKey, javacard.security.Signature.MODE_VERIFY);
        return scMac.verify(responseBuffer, (short) 0, (short) (responseBuffer.length - 16),
                responseBuffer, (short) (responseBuffer.length - 16), (short) 16);
    }

    /**
     * Perform AES decryption on response buffer
     * @param responseBuffer buffer with encrypted data
     * @return decrypted response
     */
    private byte[] aesDecrypt(byte[] responseBuffer) {
        aesCbc.init(this.aesKey, javacardx.crypto.Cipher.MODE_DECRYPT, this.iv, (short) 0, (short) 16);
        short decryptedLength = aesCbc.doFinal(responseBuffer, (short) 0, (short) (responseBuffer.length - 16), responseBuffer, (short) 0);
        byte[] decryptedResponse = new byte[decryptedLength];
        System.arraycopy(responseBuffer, 0, decryptedResponse, 0, decryptedLength);
        return decryptedResponse;
    }
}
