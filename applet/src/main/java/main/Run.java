package main;

import com.licel.jcardsim.smartcardio.CardSimulator;

import javax.crypto.*;
import java.io.IOException;
import java.security.*;

public class Run {
    static CardSimulator simulator;

    public static void main() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
//        // 1. create simulator
//        simulator = new CardSimulator();
//
//        // 2. install applet
//        AID appletAID = AIDUtil.create("F000000001");
//        simulator.installApplet(appletAID, CardSmartApplet.class);
//
//        // 3. select applet
//        simulator.selectApplet(appletAID);
//
//        // USER COMMAND LINE INPUT
//        String path = "./pairing_secret_file";
//        byte[] providedPIN = {0x31, 0x32, 0x33, 0x34, 0, 0, 0, 0, 0, 0};
//        // GLOBAL VARIABLES NEEDED FOR ENCRYPTION AND MAC
//        byte[] encryptionKey = new byte[32];
//        byte[] macKey = new byte[32];
//        byte[] iv = new byte[16];
//        /* Initialize applet workflow */
//        {
//            KeyPair keyPair = Secure.generateECKeyPair();
//            initializeApplet(providedPIN, path, keyPair, iv);
//        }
//        /* Create secure channel */
//        {
//            openSecureChannel(path, iv, encryptionKey, macKey);
//        }
//        /* First APDU after opening the secure channel, i.e. PIN verify */
//        {
//            // prepare secure APDU
//            CommandAPDU verifyAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x32, providedPIN, iv, encryptionKey, macKey);
//            // get response
//            ResponseAPDU verifyResponse = simulator.transmitCommand(verifyAPDU);
//            byte[] response = getResponseData(macKey, encryptionKey, iv, verifyResponse.getData());
//            if (Arrays.equals(response, new byte[]{-112, 0})) {
//                System.out.print("Verification successful!\n");
//            } else {
//                System.out.print("Verification not successful!\n");
//            }
//        }
//        /* Second APDU after opening the secure channel, i.e. PIN change */
//        {
//            // prepare secure APDU
//            byte[] newPIN = {0x01, 0x02, 0x03, 0x04, 0, 0, 0, 0, 0, 0};
//            CommandAPDU changePINAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x33, newPIN, iv, encryptionKey, macKey);
//            // get response
//            ResponseAPDU changePINResponse = simulator.transmitCommand(changePINAPDU);
//            byte[] response = getResponseData(macKey, encryptionKey, iv, changePINResponse.getData());
//            assert response != null;
//            if (Arrays.equals(response, new byte[]{-112, 0})) {
//                System.out.print("Change PIN successful!\n");
//            } else {
//                System.out.print("Change PIN not successful!\n");
//            }
//        }
//        /* Third APDU after opening the secure channel, i.e. new PIN verify */
//        {
//            // prepare secure APDU
//            byte[] newPIN = {0x01, 0x02, 0x03, 0x04, 0, 0, 0, 0, 0, 0};
//            CommandAPDU changePINAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x32, newPIN, iv, encryptionKey, macKey);
//            // get response
//            ResponseAPDU changePINResponse = simulator.transmitCommand(changePINAPDU);
//            byte[] response = getResponseData(macKey, encryptionKey, iv, changePINResponse.getData());
//            assert response != null;
//            if (Arrays.equals(response, new byte[]{-112, 0})) {
//                System.out.print("Verify new PIN successful!\n");
//            } else {
//                System.out.print("Verify new PIN not successful!\n");
//            }
//        }
//        { // Store secret
//            byte[] nameAndSecret = {4, 0x31, 0x32, 0x33, 0x34, 4,0x51, 0x52, 0x53, 0x54 };
//            CommandAPDU storeAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x35, nameAndSecret, iv, encryptionKey, macKey);
//            ResponseAPDU storeResponse = simulator.transmitCommand(storeAPDU);
//            byte[] response = getResponseData(macKey, encryptionKey, iv, storeResponse.getData());
//            if (Arrays.equals(response, new byte[]{-112, 0})) {
//                System.out.print("Store secret successful!\n");
//            } else {
//                System.out.print("Store secret not successful!\n");
//            }
//        }
//        { // Get secret
//            byte[] name = {0x31, 0x32, 0x33, 0x34};
//            CommandAPDU getAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x34, name, iv, encryptionKey, macKey);
//            ResponseAPDU getResponse = simulator.transmitCommand(getAPDU);
//            byte[] response = getResponseData(macKey, encryptionKey, iv, getResponse.getData());
//            if (Arrays.equals(response, new byte[]{0x51, 0x52, 0x53, 0x54, -112, 0})) {
//                System.out.print("Get secret successful!\n");
//            } else {
//                System.out.print("Get secret not successful!\n");
//            }
//        }
//        { // Get all names
//            CommandAPDU getAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x30, null, iv, encryptionKey, macKey);
//            ResponseAPDU getResponse = simulator.transmitCommand(getAPDU);
//            byte[] response = getResponseData(macKey, encryptionKey, iv, getResponse.getData());
//            if (Arrays.equals(response, new byte[]{4, 0x31, 0x32, 0x33, 0x34, -112, 0})) {
//                System.out.print("Get all names successful!\n");
//            } else {
//                System.out.print("Get all names not successful!\n");
//            }
//        }
//        { // delete secret
//            byte[] name = {0x31, 0x32, 0x33, 0x34};
//            CommandAPDU getAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x36, name, iv, encryptionKey, macKey);
//            ResponseAPDU getResponse = simulator.transmitCommand(getAPDU);
//            byte[] response = getResponseData(macKey, encryptionKey, iv, getResponse.getData());
//            if (Arrays.equals(response, new byte[]{-112, 0})) {
//                System.out.print("Delete secret successful!\n");
//            } else {
//                System.out.print("Delete secret not successful!\n");
//            }
//        }
//        { // Get all names should be empty
//            CommandAPDU getAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x30, null, iv, encryptionKey, macKey);
//            ResponseAPDU getResponse = simulator.transmitCommand(getAPDU);
//            byte[] response = getResponseData(macKey, encryptionKey, iv, getResponse.getData());
//            if (Arrays.equals(response, new byte[]{-112, 0})) {
//                System.out.print("Get all names empty successful!\n");
//            } else {
//                System.out.print("Get all names empty not successful!\n");
//            }
//        }
    }

//--------- HIGH LEVEL API---------------------------------------------------------------------------------------------
//    public static void initializeApplet(byte[] providedPIN, String path, KeyPair keyPair, byte[] iv)
//            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
//        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
//        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
//        // get card's public key
//        CommandAPDU commandAPDU = new CommandAPDU(0xB0, 0x40, 0x00, 0x00);
//        ResponseAPDU response = simulator.transmitCommand(commandAPDU);
//        byte[] apduData = response.getData();
//        ECPublicKey cardPublicKey = Secure.convertBytesToPublicKey(apduData);
//        // generate pairing secret
//        byte[] pairingSecret = Secure.generatePairingSecret();
//        // generate IV for encryption
//        Secure.generateIV(iv);
//        // derived shared secret as key for encryption
//        byte[] derivedSecret = Secure.getDerivedSecret(cardPublicKey, privateKey);
//        // encrypt [PIN | pairingSecret]
//        byte[] data = new byte[42];
//        System.arraycopy(providedPIN, 0, data, 0, providedPIN.length);
//        System.arraycopy(pairingSecret, 0, data, providedPIN.length, pairingSecret.length);
//        byte[] encrypted = Secure.aesEncrypt(data, iv, derivedSecret);
//        // init command APDU: 0xB0 | 0x41 | 0x00 | 0x00 | 0x81 | publicKey [65 B] | IV [16 B] | encrypted [48 B]
//        byte[] payload = new byte[129];
//        byte[] publicKeyBytes = Secure.getPublicKeyBytes(publicKey);
//        // copy into APDU data part
//        System.arraycopy(publicKeyBytes, 0, payload, 0, publicKeyBytes.length);
//        System.arraycopy(iv, 0, payload, publicKeyBytes.length, iv.length);
//        System.arraycopy(encrypted, 0, payload, publicKeyBytes.length + iv.length, encrypted.length);
//        CommandAPDU initAPDU = new CommandAPDU(0xB0, 0x41, 0x00, 0x00, payload);
//        // get response from card
//        ResponseAPDU initResponse = simulator.transmitCommand(initAPDU);
//        if (initResponse.getSW() == 0x9000)
//            System.out.print("Success to init applet!\n");
//        else {
//            System.out.print("Failed to init applet!\n");
//            return;
//        }
//        // write pairingSecret into file
//        Util.writeIntoFile(path, pairingSecret);
//    }
//
//    public static void openSecureChannel(String path, byte[] iv, byte[] encryptionKey, byte[] macKey)
//            throws IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException {
//        // get and extract pairing secret from path
//        byte[] pairingSecret = Util.readFromFile(path);
//        // get public key from card
//        CommandAPDU getKeyCommandAPDU = new CommandAPDU(0xB0, 0x40, 0x00, 0x00);
//        ResponseAPDU getKeyResponse = simulator.transmitCommand(getKeyCommandAPDU);
//        // convert bytes into public key
//        byte[] apduData = getKeyResponse.getData();
//        ECPublicKey cardPublicKey = Secure.convertBytesToPublicKey(apduData);
//        // tool generates ephemeral key
//        KeyPair keyPair = Secure.generateECKeyPair();
//        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
//        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
//        byte[] publicKeyBytes = Secure.getPublicKeyBytes(publicKey);
//        // tool sends Open Secure Channel Command APDU with public key
//        CommandAPDU openSCCommandAPDU = new CommandAPDU(0xB0, 0x42, 0x00, 0x00, publicKeyBytes);
//        ResponseAPDU openSCResponse = simulator.transmitCommand(openSCCommandAPDU);
//        if (openSCResponse.getSW() == 0x9000)
//            System.out.print("Success to open SC!\n");
//        else {
//            System.out.print("Failed to open SC!\n");
//            return;
//        }
//        // parse returned salt and IV
//        byte[] salt = new byte[32];
//        System.arraycopy(openSCResponse.getData(), 0, salt, 0, salt.length);
//        System.arraycopy(openSCResponse.getData(), salt.length, iv, 0, iv.length);
//        // encryption and MAC keys
//        byte[] sharedSecrets = Secure.computeSharedSecrets(cardPublicKey, privateKey, pairingSecret, salt);
//        System.arraycopy(sharedSecrets, 0, encryptionKey, 0, encryptionKey.length);
//        System.arraycopy(sharedSecrets, encryptionKey.length, macKey, 0, macKey.length);
//    }
//
//    /**
//     * Prepare APDU with encrypted payload and appended MAC tag
//     * @param CLA CLA byte
//     * @param INS INS byte
//     * @param data data to be sent
//     * @param iv IV for encryption and MAC
//     * @param key key for encryption
//     * @param macKey key for MAC
//     * @return prepared CommandAPDU object
//     */
//    public static CommandAPDU prepareSecureAPDU(byte CLA, byte INS, byte[] data, byte[] iv, byte[] key, byte[] macKey)
//            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
//        byte[] apduBuffer = null;
//        short filledLength = 0;
//        if (data != null) {
//            // no data part in APDU
//            byte[] encryptedPayload = Secure.aesEncrypt(data, iv, key);
//            apduBuffer = new byte[5 + encryptedPayload.length + 16];
//            apduBuffer[4] = (byte) (encryptedPayload.length + 16); // payload + MAC size
//            System.arraycopy(encryptedPayload, 0, apduBuffer, 5, encryptedPayload.length);
//            filledLength += encryptedPayload.length;
//        } else {
//            apduBuffer = new byte[5 + 16];
//            apduBuffer[4] = (byte) 16;
//        }
//
//        // prepare temporary buffer with prepended instructions
//        apduBuffer[0] = CLA;
//        apduBuffer[1] = INS;
//        apduBuffer[2] = apduBuffer[3] = 0; // P1, P2
//        filledLength += 5;
//        // compute MAC and append after payload in apduBuffer
//        Secure.computeMacAndAppend(apduBuffer, filledLength, macKey, iv);
//        // set iv for next decryption
//        Secure.setIV(iv, apduBuffer, (short) (apduBuffer.length - 16));
//        // send to card
//        return new CommandAPDU(apduBuffer);
//    }
//
//    /**
//     * Get byte array of response data: payload | SW1 | SW2
//     * @param macKey key for MAC verification
//     * @param encryptionKey key for decryption
//     * @param iv IV for decryption
//     * @param responseData encrypted data buffer
//     * @return byte response
//     */
//    public static byte[] getResponseData(byte[] macKey, byte[] encryptionKey, byte[] iv, byte[] responseData) {
//        // verify MAC tag
//        boolean verified = Secure.verifyResponseMAC(macKey, responseData);
//        if (!verified) {
//            System.out.println("MAC not verified!");
//            return null;
//        } else {
//            System.out.println("MAC verified!");
//        }
//        // decrypt payload
//        // TODO: not working decryption
//        byte[] decrypted = Secure.aesDecrypt(responseData, encryptionKey, iv);
//        if (decrypted.length > 0) {
//            System.out.println("Decrypted payload!");
//        }
//        // set MAC as new iv for next encryption
//        Secure.setIV(iv, responseData, (short) (responseData.length - 16));
//        return decrypted;
//    }
 }