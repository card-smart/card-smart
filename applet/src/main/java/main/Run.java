package main;

import applet.CardSmartApplet;
import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;
import javacard.framework.AID;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.smartcardio.*;
import java.io.FileInputStream;
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

import javacard.framework.ISO7816;

import javacard.security.AESKey;
import javacard.security.KeyBuilder;
import javacard.security.Signature;

public class Run {
    static CardSimulator simulator;

    public static void main() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
        // 1. create simulator
         simulator = new CardSimulator();

        // 2. install applet
        AID appletAID = AIDUtil.create("F000000001");
        simulator.installApplet(appletAID, CardSmartApplet.class);

        // 3. select applet
        simulator.selectApplet(appletAID);

        // USER COMMAND LINE INPUT
        String path = "./pairing_secret_file";
        byte[] providedPIN = {0x31, 0x32, 0x33, 0x34, 0, 0, 0, 0, 0, 0};
        // GLOBAL VARIABLES NEEDED FOR ENCRYPTION AND MAC
        byte[] encryptionKey = new byte[32];
        byte[] macKey = new byte[32];
        byte[] iv = new byte[16];
        /* Initialize applet workflow */
        {
            KeyPair keyPair = generateECKeyPair();
            initializeApplet(providedPIN, path, keyPair, iv);
        }
        /* Create secure channel */
        {
            openSecureChannel(path, iv, encryptionKey, macKey);
        }
        /* First APDU after opening the secure channel, i.e. PIN verify */
        {
            // prepare secure APDU
            CommandAPDU verifyAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x32, providedPIN, iv, encryptionKey, macKey);
            // get response
            ResponseAPDU verifyResponse = simulator.transmitCommand(verifyAPDU);
            byte[] response = getResponseData(macKey, encryptionKey, iv, verifyResponse.getData());
            if (Arrays.equals(response, new byte[]{-112, 0})) {
                System.out.print("Verification successful!\n");
            } else {
                System.out.print("Verification not successful!\n");
            }
        }
        /* Second APDU after opening the secure channel, i.e. PIN change */
        {
            // prepare secure APDU
            byte[] newPIN = {0x01, 0x02, 0x03, 0x04, 0, 0, 0, 0, 0, 0};
            CommandAPDU changePINAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x33, newPIN, iv, encryptionKey, macKey);
            // get response
            ResponseAPDU changePINResponse = simulator.transmitCommand(changePINAPDU);
            byte[] response = getResponseData(macKey, encryptionKey, iv, changePINResponse.getData());
            assert response != null;
            if (Arrays.equals(response, new byte[]{-112, 0})) {
                System.out.print("Change PIN successful!\n");
            } else {
                System.out.print("Change PIN not successful!\n");
            }
        }
        /* Third APDU after opening the secure channel, i.e. new PIN verify */
        {
            // prepare secure APDU
            byte[] newPIN = {0x01, 0x02, 0x03, 0x04, 0, 0, 0, 0, 0, 0};
            CommandAPDU changePINAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x32, newPIN, iv, encryptionKey, macKey);
            // get response
            ResponseAPDU changePINResponse = simulator.transmitCommand(changePINAPDU);
            byte[] response = getResponseData(macKey, encryptionKey, iv, changePINResponse.getData());
            assert response != null;
            if (Arrays.equals(response, new byte[]{-112, 0})) {
                System.out.print("Verify new PIN successful!\n");
            } else {
                System.out.print("Verify new PIN not successful!\n");
            }
        }
        { // Store secret
            byte[] nameAndSecret = {4, 0x31, 0x32, 0x33, 0x34, 4,0x51, 0x52, 0x53, 0x54 };
            CommandAPDU storeAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x35, nameAndSecret, iv, encryptionKey, macKey);
            ResponseAPDU storeResponse = simulator.transmitCommand(storeAPDU);
            byte[] response = getResponseData(macKey, encryptionKey, iv, storeResponse.getData());
            if (Arrays.equals(response, new byte[]{-112, 0})) {
                System.out.print("Store secret successful!\n");
            } else {
                System.out.print("Store secret not successful!\n");
            }
        }
        { // Get secret
            byte[] name = {0x31, 0x32, 0x33, 0x34};
            CommandAPDU getAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x34, name, iv, encryptionKey, macKey);
            ResponseAPDU getResponse = simulator.transmitCommand(getAPDU);
            byte[] response = getResponseData(macKey, encryptionKey, iv, getResponse.getData());
            if (Arrays.equals(response, new byte[]{0x51, 0x52, 0x53, 0x54, -112, 0})) {
                System.out.print("Get secret successful!\n");
            } else {
                System.out.print("Get secret not successful!\n");
            }
        }
        { // Get all names
            CommandAPDU getAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x30, null, iv, encryptionKey, macKey);
            ResponseAPDU getResponse = simulator.transmitCommand(getAPDU);
            byte[] response = getResponseData(macKey, encryptionKey, iv, getResponse.getData());
            if (Arrays.equals(response, new byte[]{4, 0x31, 0x32, 0x33, 0x34, -112, 0})) {
                System.out.print("Get all names successful!\n");
            } else {
                System.out.print("Get all names not successful!\n");
            }
        }
        { // delete secret
            byte[] name = {0x31, 0x32, 0x33, 0x34};
            CommandAPDU getAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x36, name, iv, encryptionKey, macKey);
            ResponseAPDU getResponse = simulator.transmitCommand(getAPDU);
            byte[] response = getResponseData(macKey, encryptionKey, iv, getResponse.getData());
            if (Arrays.equals(response, new byte[]{-112, 0})) {
                System.out.print("Delete secret successful!\n");
            } else {
                System.out.print("Delete secret not successful!\n");
            }
        }
        { // Get all names should be empty
            CommandAPDU getAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x30, null, iv, encryptionKey, macKey);
            ResponseAPDU getResponse = simulator.transmitCommand(getAPDU);
            byte[] response = getResponseData(macKey, encryptionKey, iv, getResponse.getData());
            if (Arrays.equals(response, new byte[]{-112, 0})) {
                System.out.print("Get all names empty successful!\n");
            } else {
                System.out.print("Get all names empty not successful!\n");
            }
        }
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
     * Generate 16B IV
     * @return random data
     */
    public static byte[] generateIV() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return randomBytes;
    }

    /**
     * Derive 32B secret value using ECDH
     * @param cardPublicKey public key received from card converted into ECPublicKey
     * @param toolPrivateKey ephemeral private key of tool
     * @return 32B derived secret value
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public static byte[] getDerivedSecret(ECPublicKey cardPublicKey, ECPrivateKey toolPrivateKey) throws NoSuchAlgorithmException, InvalidKeyException {
        KeyAgreement ecdh = KeyAgreement.getInstance("ECDH");
        ecdh.init(toolPrivateKey);
        ecdh.doPhase(cardPublicKey, true);
        return ecdh.generateSecret();
    }

    /**
     * Custom implementation of
     * @param data
     * @return
     */
    private static byte[] padISO9797_M2(byte[] data) {
        int paddingLength = 16 - (data.length % 16);
        byte[] paddedData = new byte[data.length + paddingLength];
        Arrays.fill(paddedData, (byte) 0);
        System.arraycopy(data, 0, paddedData, 0, data.length);
        paddedData[data.length] = (byte) 0x80;
        return paddedData;
    }

    /**
     * Perform AES encryption on given data
     * @param data data to encrypt, not padded
     * @param ivBytes IV value in bytes, 16B
     * @param keyBytes key value in bytes, 32B
     * @return encrypted array of bytes
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
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
     * Convert ECPublicKey object into byte array representing uncompressed EC point
     * @param publicKey public key for conversion
     * @return public key as uncompressed bytes
     */
    private static byte[] getPublicKeyBytes(ECPublicKey publicKey) {
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
     * Write byte data into file
     * @param path path of file to be written into
     * @param data data to be written
     */
    private static void writeIntoFile(String path, byte[] data) throws IOException {
        try (FileOutputStream output = new FileOutputStream(path)) {
            output.write(data);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Read byte array from file
     * @param path path to file to be read
     * @return read byte array
     */
    private static byte[] readFromFile(String path) throws IOException {
        FileInputStream input = null;
        byte[] data = null;
        try {
            input = new FileInputStream(path);
            data = new byte[input.available()];
            input.read(data);
        } finally {
            if (input != null) {
                input.close();
            }
        }
        return data;
    }

    /**
     * Compute encryption and MAC key in same way as card does
     * @param cardPublicKey public key received from card
     * @param privateKey tool's private key
     * @param pairingSecret secret established by init
     * @param salt salt from opening of the secure channel
     * @return 64 B containing chained encryption and MAC key
     */
    private static byte[] computeSharedSecrets(ECPublicKey cardPublicKey, ECPrivateKey privateKey,
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
     * Compute MAC tag from 5 byte of APDU (instructions), 11 bytes from aux buffer and encrypted payload
     * @param apduBuffer prepared buffer with instructions and encrypted payload
     * @param length count of bytes in buffer
     * @param key MAC key
     * @param aux buffer for padding
     */
    private static void computeMacAndAppend(byte[] apduBuffer, short length, byte[] key, byte[] aux)  {
        Signature scMac = Signature.getInstance(Signature.ALG_AES_MAC_128_NOPAD, false);
        AESKey macKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_AES_256, false);
        macKey.setKey(key, (short) 0);
        scMac.init(macKey, Signature.MODE_SIGN);
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
    private static boolean verifyResponseMAC(byte[] key, byte[] responseBuffer) {
        Signature scMac = Signature.getInstance(Signature.ALG_AES_MAC_128_NOPAD, false);
        AESKey macKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_AES_256, false);
        macKey.setKey(key, (short) 0);
        scMac.init(macKey, Signature.MODE_VERIFY);
        return scMac.verify(responseBuffer, (short) 0, (short) (responseBuffer.length - 16),
                responseBuffer, (short) (responseBuffer.length - 16), (short) 16);
    }

    /**
     * Set new IV for encryption, decryption and MACing
     * @param iv old IV
     * @param newIv new IV
     * @param newIvOffset offset of the new IV
     */
    private static void setIV(byte[] iv, byte[] newIv, short newIvOffset) {
        System.arraycopy(newIv, newIvOffset, iv, 0, iv.length);
    }

    private static byte[] unpadISO9797_M2(byte[] input) {
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
     * Perform AES decryption on response buffer
     * @param responseBuffer buffer with encrypted data
     * @param keyBytes bytes of key
     * @param ivBytes bytes of iv
     * @return decrypted response
     */
    private static byte[] aesDecrypt(byte[] responseBuffer, byte[] keyBytes, byte[] ivBytes) {
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

//--------- HIGH LEVEL API---------------------------------------------------------------------------------------------
    public static void initializeApplet(byte[] providedPIN, String path, KeyPair keyPair, byte[] iv)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        // get card's public key
        CommandAPDU commandAPDU = new CommandAPDU(0xB0, 0x40, 0x00, 0x00);
        ResponseAPDU response = simulator.transmitCommand(commandAPDU);
        byte[] apduData = response.getData();
        ECPublicKey cardPublicKey = convertBytesToPublicKey(apduData);
        // generate pairing secret
        byte[] pairingSecret = generatePairingSecret();
        // generate IV for encryption
        generateIV(iv);
        // derived shared secret as key for encryption
        byte[] derivedSecret = getDerivedSecret(cardPublicKey, privateKey);
        // encrypt [PIN | pairingSecret]
        byte[] data = new byte[42];
        System.arraycopy(providedPIN, 0, data, 0, providedPIN.length);
        System.arraycopy(pairingSecret, 0, data, providedPIN.length, pairingSecret.length);
        byte[] encrypted = aesEncrypt(data, iv, derivedSecret);
        // init command APDU: 0xB0 | 0x41 | 0x00 | 0x00 | 0x81 | publicKey [65 B] | IV [16 B] | encrypted [48 B]
        byte[] payload = new byte[129];
        byte[] publicKeyBytes = getPublicKeyBytes(publicKey);
        // copy into APDU data part
        System.arraycopy(publicKeyBytes, 0, payload, 0, publicKeyBytes.length);
        System.arraycopy(iv, 0, payload, publicKeyBytes.length, iv.length);
        System.arraycopy(encrypted, 0, payload, publicKeyBytes.length + iv.length, encrypted.length);
        CommandAPDU initAPDU = new CommandAPDU(0xB0, 0x41, 0x00, 0x00, payload);
        // get response from card
        ResponseAPDU initResponse = simulator.transmitCommand(initAPDU);
        if (initResponse.getSW() == 0x9000)
            System.out.print("Success to init applet!\n");
        else {
            System.out.print("Failed to init applet!\n");
            return;
        }
        // write pairingSecret into file
        writeIntoFile(path, pairingSecret);
    }

    public static void openSecureChannel(String path, byte[] iv, byte[] encryptionKey, byte[] macKey)
            throws IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException {
        // get and extract pairing secret from path
        byte[] pairingSecret = readFromFile(path);
        // get public key from card
        CommandAPDU getKeyCommandAPDU = new CommandAPDU(0xB0, 0x40, 0x00, 0x00);
        ResponseAPDU getKeyResponse = simulator.transmitCommand(getKeyCommandAPDU);
        // convert bytes into public key
        byte[] apduData = getKeyResponse.getData();
        ECPublicKey cardPublicKey = convertBytesToPublicKey(apduData);
        // tool generates ephemeral key
        KeyPair keyPair = generateECKeyPair();
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        byte[] publicKeyBytes = getPublicKeyBytes(publicKey);
        // tool sends Open Secure Channel Command APDU with public key
        CommandAPDU openSCCommandAPDU = new CommandAPDU(0xB0, 0x42, 0x00, 0x00, publicKeyBytes);
        ResponseAPDU openSCResponse = simulator.transmitCommand(openSCCommandAPDU);
        if (openSCResponse.getSW() == 0x9000)
            System.out.print("Success to open SC!\n");
        else {
            System.out.print("Failed to open SC!\n");
            return;
        }
        // parse returned salt and IV
        byte[] salt = new byte[32];
        System.arraycopy(openSCResponse.getData(), 0, salt, 0, salt.length);
        System.arraycopy(openSCResponse.getData(), salt.length, iv, 0, iv.length);
        // encryption and MAC keys
        byte[] sharedSecrets = computeSharedSecrets(cardPublicKey, privateKey, pairingSecret, salt);
        System.arraycopy(sharedSecrets, 0, encryptionKey, 0, encryptionKey.length);
        System.arraycopy(sharedSecrets, encryptionKey.length, macKey, 0, macKey.length);
    }

    /**
     * Prepare APDU with encrypted payload and appended MAC tag
     * @param CLA CLA byte
     * @param INS INS byte
     * @param data data to be sent
     * @param iv IV for encryption and MAC
     * @param key key for encryption
     * @param macKey key for MAC
     * @return prepared CommandAPDU object
     */
    public static CommandAPDU prepareSecureAPDU(byte CLA, byte INS, byte[] data, byte[] iv, byte[] key, byte[] macKey)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        byte[] apduBuffer = null;
        short filledLength = 0;
        if (data != null) {
            // no data part in APDU
            byte[] encryptedPayload = aesEncrypt(data, iv, key);
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
        computeMacAndAppend(apduBuffer, filledLength, macKey, iv);
        // set iv for next decryption
        setIV(iv, apduBuffer, (short) (apduBuffer.length - 16));
        // send to card
        return new CommandAPDU(apduBuffer);
    }

    /**
     * Get byte array of response data: payload | SW1 | SW2
     * @param macKey key for MAC verification
     * @param encryptionKey key for decryption
     * @param iv IV for decryption
     * @param responseData encrypted data buffer
     * @return byte response
     */
    public static byte[] getResponseData(byte[] macKey, byte[] encryptionKey, byte[] iv, byte[] responseData) {
        // verify MAC tag
        boolean verified = verifyResponseMAC(macKey, responseData);
        if (!verified) {
            System.out.print("MAC not verified!\n");
            return null;
        } else {
            System.out.print("MAC verified!\n");
        }
        // decrypt payload
        // TODO: not working decryption
        byte[] decrypted = aesDecrypt(responseData, encryptionKey, iv);
        if (decrypted.length > 0) {
            System.out.print("Decrypted payload!\n");
        }
        // set MAC as new iv for next encryption
        setIV(iv, responseData, (short) (responseData.length - 16));
        return decrypted;
    }
 }