package tool;

import javacard.security.AESKey;
import javacard.security.KeyBuilder;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileNotFoundException;
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

public class Secure {
    PublicKey publicKey;
    PrivateKey privateKey;

    public static byte[] createSecret(String path) {
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

    public void generateEC() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        // TODO catching exceptions
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
        keyGen.initialize(ecSpec, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
    }

    /**
     * Convert ECPublicKey object into byte array representing uncompressed EC point
     * @param publicKey public key for conversion
     * @return public key as uncompressed bytes
     */
    public static byte[] getPublicKeyBytes(ECPublicKey publicKey) {
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
     * @apiNote https://stackoverflow.com/questions/26159149/how-can-i-get-a-publickey-object-from-ec-public-key-bytes
     */
    public static ECPublicKey convertBytesToPublicKey(byte[] ecPoint) {
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
            ECPublicKey key = (ECPublicKey)kf.generatePublic(pubSpec);
            return key;
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    /**
     * Generate tool ephemeral keypair over secp256r1 curve
     * @return generated keypair
     * @throws NoSuchAlgorithmException
     */
    public static KeyPair generateECKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();
        return keyPair;
    }

    /**
     * Generate 32B pairing secret
     * @return random data
     */
    public static byte[] generatePairingSecret() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return randomBytes;
    }

    /**
     * Derive 32B secret value using ECDH
     * @param cardPublicKey public key received from card converted into ECPublicKey
     * @param toolPrivateKey ephemeral private key of tool
     * @return 32B derived secret value
     */
    public static byte[] getDerivedSecret(ECPublicKey cardPublicKey, ECPrivateKey toolPrivateKey) throws NoSuchAlgorithmException, InvalidKeyException {
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
    public static byte[] computeSharedSecrets(ECPublicKey cardPublicKey, ECPrivateKey privateKey,
                                              byte[] pairingSecret, byte[] salt)
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
    public static void generateIV(byte[] iv) {
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
    }

    /**
     * Set new IV for encryption, decryption and MACing
     * @param iv old IV
     * @param newIv new IV
     * @param newIvOffset offset of the new IV
     */
    public static void setIV(byte[] iv, byte[] newIv, short newIvOffset) {
        System.arraycopy(newIv, newIvOffset, iv, 0, iv.length);
    }

    /**
     * Custom implementation of ISO9797 M2 padding
     * @param data data to be padded
     * @return padded data
     */
    public static byte[] padISO9797_M2(byte[] data) {
        int paddingLength = 16 - (data.length % 16);
        byte[] paddedData = new byte[data.length + paddingLength];
        Arrays.fill(paddedData, (byte) 0);
        System.arraycopy(data, 0, paddedData, 0, data.length);
        paddedData[data.length] = (byte) 0x80;
        return paddedData;
    }

    public static byte[] unpadISO9797_M2(byte[] input) {
        int paddingLength = 0;
        for (int i = input.length - 1; i >= 0; i--) {
            if (input[i] == (byte) 0x80) {
                paddingLength = input.length - i - 1;
                break;
            }
        }
        byte[] output = new byte[input.length - paddingLength];
        System.arraycopy(input, 0, output, 0, output.length);
        return output;
    }

    /**
     * Perform AES encryption on given data
     * @param data data to encrypt, not padded
     * @param ivBytes IV value in bytes, 16B
     * @param keyBytes key value in bytes, 32B
     * @return encrypted array of bytes
     */
    public static byte[] aesEncrypt(byte[] data, byte[] ivBytes, byte[] keyBytes)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        byte[] paddedData = padISO9797_M2(data);

        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        return cipher.doFinal(paddedData);
    }

    /**
     * Compute MAC tag from 5 byte of APDU (instructions), 11 bytes from aux buffer and encrypted payload
     * @param apduBuffer prepared buffer with instructions and encrypted payload
     * @param length count of bytes in buffer
     * @param key MAC key
     * @param aux buffer for padding
     */
    public static void computeMacAndAppend(byte[] apduBuffer, short length, byte[] key, byte[] aux)  {
        javacard.security.Signature scMac = javacard.security.Signature.getInstance(javacard.security.Signature.ALG_AES_MAC_128_NOPAD, false);
        AESKey macKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_AES_256, false);
        macKey.setKey(key, (short) 0);
        scMac.init(macKey, javacard.security.Signature.MODE_SIGN);
        scMac.update(apduBuffer, (short) 0, (short) 5); // first 5 instruction bytes
        scMac.update(aux, (short) 0, (short) 11); // pad them with some values
        scMac.sign(apduBuffer, (short) 5, (short) (length - 5), apduBuffer, length);
    }

    /**
     * Verify MAC over payload in response buffer
     * @param key MAC secret shared key
     * @param responseBuffer buffer with payload
     * @return true if verified, false otherwise
     */
    public static boolean verifyResponseMAC(byte[] key, byte[] responseBuffer) {
        javacard.security.Signature scMac = javacard.security.Signature.getInstance(javacard.security.Signature.ALG_AES_MAC_128_NOPAD, false);
        AESKey macKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_AES_256, false);
        macKey.setKey(key, (short) 0);
        scMac.init(macKey, javacard.security.Signature.MODE_VERIFY);
        return scMac.verify(responseBuffer, (short) 0, (short) (responseBuffer.length - 16),
                responseBuffer, (short) (responseBuffer.length - 16), (short) 16);
    }

    /**
     * Perform AES decryption on response buffer
     * @param responseBuffer buffer with encrypted data
     * @param keyBytes bytes of key
     * @param ivBytes bytes of iv
     * @return decrypted response
     */
    public static byte[] aesDecrypt(byte[] responseBuffer, byte[] keyBytes, byte[] ivBytes) {
        javacardx.crypto.Cipher aesCbc = javacardx.crypto.Cipher.getInstance(javacardx.crypto.Cipher.ALG_AES_CBC_ISO9797_M2, false);
        AESKey key = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_256, false);
        key.setKey(keyBytes, (short) 0);
        aesCbc.init(key, javacardx.crypto.Cipher.MODE_DECRYPT, ivBytes, (short) 0, (short) 16);

        // TODO: not working decryption - padded block corrupted
        short decryptedLength = aesCbc.doFinal(responseBuffer, (short) 0, (short) (responseBuffer.length - 16), responseBuffer, (short) 0);
        byte[] decryptedResponse = new byte[decryptedLength];
        System.arraycopy(responseBuffer, 0, decryptedResponse, 0, decryptedLength);
        return decryptedResponse;
    }
}
