package tool;

import cardTools.CardManager;
import cardTools.RunConfig;
import cardTools.Util;
import main.Run;
import org.apache.commons.cli.CommandLine;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

public class Main {
    private static final Run run = new Run();
    private static final CommandParser cmdParser = new CommandParser();
    private static final boolean release = false;
    private static final boolean simulator = false;
    private static final String APPLET_AID = "63617264736D6172746170706C6574";
    private static final byte[] APPLET_AID_BYTE = Util.hexStringToByteArray(APPLET_AID);

    // demo to try initApplet, openSC and secure verifyPIN on real card
    public static void demo() throws Exception {
        // USER COMMAND LINE INPUT
        String path = "./pairing_secret_file";
        byte[] providedPIN = {0x31, 0x32, 0x33, 0x34, 0, 0, 0, 0, 0, 0};
        // GLOBAL VARIABLES NEEDED FOR ENCRYPTION AND MAC
        byte[] encryptionKey = new byte[32];
        byte[] macKey = new byte[32];
        byte[] iv = new byte[16];

        final CardManager cardMngr = cardSelectApplet();
        if (cardMngr == null) {
            return;
        }

        /* Initialize applet workflow */
        {
            KeyPair keyPair = Secure.generateECKeyPair();
            initializeApplet(cardMngr, providedPIN, path, keyPair, iv);
        }
        /* Create secure channel */
        {
            openSecureChannel(cardMngr, path, iv, encryptionKey, macKey);
        }
        /* First APDU after opening the secure channel, i.e. PIN verify */
        {
            // prepare secure APDU
            CommandAPDU verifyAPDU = prepareSecureAPDU((byte) 0xB0, (byte) 0x32, providedPIN, iv, encryptionKey, macKey);
            // get response
            ResponseAPDU verifyResponse = cardMngr.transmit(verifyAPDU);
            byte[] response = getResponseData(macKey, encryptionKey, iv, verifyResponse.getData());
        }
    }

    public static void main(String[] args) throws Exception {
        //run.main();
        Main.demo();

//
//        /*
//        This is for our testing - on simulator or on a real card - specified above,
//        simulator is the default and only call, as the real card is not implemented yet
//
//        You can use either sendAPDU() function where you can specify
//        command line arguments in an array to avoid writing them each time:
//
//            sendAPDU(cmdParser, new String[]{"-v", "meno", "--pin", "pinik"});
//
//        OR you can use smartie function to test more behaviour.
//        It will run smartie app and expect input until you type 'quit':
//
//            smartie$ -v name --pin 75436
//            // some output
//            smartie$ --list
//            // some output
//            smartie$ quit
//         */
//        if (!release) {
//            sendAPDU(new String[]{"-v", "meno", "--pin", "pinik"});
//            //smartie();
//            return;
//        }
//
//        // THIS IS FOR RELEASE, NOT TESTING
//        if (args.length > 0) {
//            CommandLine cmd_parsed = cmdParser.parse(cmdParser.options, args);
//            // TODO call desired instruction
//            return;
//        }
//
//        smartie();

    }

    private static void simulator(Arguments args, CommandLine cmd) {
        //run.getTries();
        if (args.loginNeeded) {
            //run.login(args);
        }
        if (cmd.hasOption('c')) {
            //run.changePIN(args);
        }
    }

    private static void sendAPDU(String[] cmd) throws Exception {
        CommandLine cmd_parsed = cmdParser.parse(cmdParser.options, cmd);
        Arguments args = new Arguments(cmd_parsed);

        if (!args.validateInput()) {
            return;
        }

        if (simulator) {
            simulator(args, cmd_parsed);
            return;
        }


        final CardManager cardMngr = cardSelectApplet();
        if (cardMngr == null) {
            return;
        }

        if (args.loginNeeded) {
            if (!cardGetPINTries(cardMngr)) {
                System.out.println("You exceeded the possible tries for PIN, card is blocked");
            }
            if (cardVerifyPINOnly(cardMngr, args) != 0) {
                return;
            }
        }

        if (cmd_parsed.hasOption('l'))
            cardGetNames(cardMngr);
        else if (cmd_parsed.hasOption('v'))
            cardGetSecret(cardMngr, args);
        else if (cmd_parsed.hasOption('c'))
            cardChangePIN(cardMngr, args);
        else if (cmd_parsed.hasOption('s'))
            cardStoreSecret(cardMngr, args);
        else if (cmd_parsed.hasOption('d'))
            cardDeleteSecret(cardMngr, args);
        else if (cmd_parsed.hasOption('p')) // this optin has to be always the last
            cardVerifyPIN(cardMngr, args);

        //cardGetPINTries();
        //cardVerifyPIN(); //with x30,x30,x30,x30
        //cardChangePIN(); //to x31,x30,x30,x30
        //cardStoreSecret();
        //cardGetNames();
        //cardDeleteSecret();
        //cardGetNames();
    }

    private static void smartie() throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("smartie$ ");
        String line;

        while (!Objects.equals((line = scanner.nextLine()), "quit")) {
            String[] cmd = line.split(" ");
            sendAPDU(cmd);

            System.out.print("smartie$ ");
        }
    }

    private static CardManager cardSelectApplet() throws Exception {
        CardManager cardMngr = new CardManager(true, APPLET_AID_BYTE);
        final RunConfig runCfg = RunConfig.getDefaultConfig();
        runCfg.setTestCardType(RunConfig.CARD_TYPE.PHYSICAL); // Use real card
        // Connect to first available card
        // NOTE: selects target applet based on AID specified in CardManager constructor
        System.out.print("Connecting to card...");
        if (!cardMngr.Connect(runCfg)) {
            System.out.println(" Failed.");
            return null;
        }
        System.out.println(" Done.");
        return cardMngr;
    }

    private static CommandAPDU buildAPDU(int ins, byte[] data) {
        return new CommandAPDU(0xB0, ins, 0x00, 0x00, data);
    }

    private static void cardGetNames(CardManager cardMngr) throws Exception {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x20, new byte[]{}));
        System.out.println(response); // TODO do something with data
    }

    private static boolean cardGetPINTries(CardManager cardMngr) throws Exception {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x21, new byte[]{}));
        System.out.println(response);
        // TODO validate we have enough PIN tries
        return true;
    }

    private static int cardVerifyPINOnly(CardManager cardMngr, Arguments args) throws CardException {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x22, args.PIN));
        if (response.getSW() != 0x9000) {
            System.out.print("Error verify pin TODO");
            return 1;
        }
        return 0;
    }

    private static void cardVerifyPIN(CardManager cardMngr, Arguments args) throws Exception {
        if (cardVerifyPINOnly(cardMngr, args) != 0) {
            // TODO something here
            return;
        }
    }

    private static void cardChangePIN(CardManager cardMngr, Arguments args) throws Exception {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x23, args.PIN));
        System.out.println(response);
        // TODO do something with data
    }

    private static void cardGetSecret(CardManager cardMngr, Arguments args) throws Exception {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x24, args.secretName));
        System.out.println(response);
    }

    private static void cardStoreSecret(CardManager cardMngr, Arguments args) throws Exception {
        byte[] r = Arguments.concat(new byte[]{(byte) args.secretName.length}, args.secretName,
                new byte[]{(byte) args.secretValue.length}, args.secretValue);
        byte[] data = Arrays.copyOf(r, 44);

        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x25, data));
        System.out.println(response);
    }

    private static void cardDeleteSecret(CardManager cardMngr, Arguments args) throws Exception {
        ResponseAPDU response = cardMngr.transmit(buildAPDU( 0x26, args.secretName));
        System.out.println(response);
    }

    public static void initializeApplet(CardManager cardMngr, byte[] providedPIN, String path, KeyPair keyPair, byte[] iv)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, CardException {
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        // get card's public key
        // TODO make method for this
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x40, new byte[]{}));
        byte[] apduData = response.getData();
        ECPublicKey cardPublicKey = Secure.convertBytesToPublicKey(apduData);
        // generate pairing secret
        byte[] pairingSecret = Secure.generatePairingSecret();
        // generate IV for encryption
        Secure.generateIV(iv);
        // derived shared secret as key for encryption
        byte[] derivedSecret = Secure.getDerivedSecret(cardPublicKey, privateKey);
        // encrypt [PIN | pairingSecret]
        byte[] data = new byte[42];
        System.arraycopy(providedPIN, 0, data, 0, providedPIN.length);
        System.arraycopy(pairingSecret, 0, data, providedPIN.length, pairingSecret.length);
        byte[] encrypted = Secure.aesEncrypt(data, iv, derivedSecret);
        // init command APDU: 0xB0 | 0x41 | 0x00 | 0x00 | 0x81 | publicKey [65 B] | IV [16 B] | encrypted [48 B]
        byte[] payload = new byte[129];
        byte[] publicKeyBytes = Secure.getPublicKeyBytes(publicKey);
        // copy into APDU data part
        System.arraycopy(publicKeyBytes, 0, payload, 0, publicKeyBytes.length);
        System.arraycopy(iv, 0, payload, publicKeyBytes.length, iv.length);
        System.arraycopy(encrypted, 0, payload, publicKeyBytes.length + iv.length, encrypted.length);

        // get response from card
        // TODO make method for this
        ResponseAPDU initResponse = cardMngr.transmit(buildAPDU(0x41, payload));
        if (initResponse.getSW() == 0x9000)
            System.out.print("Success to init applet!\n");
        else {
            System.out.print("Failed to init applet!\n");
            return;
        }
        // write pairingSecret into file
        tool.Util.writeIntoFile(path, pairingSecret);
    }

    public static void openSecureChannel(CardManager cardMngr, String path, byte[] iv, byte[] encryptionKey, byte[] macKey)
            throws IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException, CardException {
        // get and extract pairing secret from path
        byte[] pairingSecret = tool.Util.readFromFile(path);
        // get public key from card
        // TODO make method for this
        ResponseAPDU getKeyResponse = cardMngr.transmit(buildAPDU(0x40, new byte[]{}));
        // convert bytes into public key
        byte[] apduData = getKeyResponse.getData();
        ECPublicKey cardPublicKey = Secure.convertBytesToPublicKey(apduData);
        // tool generates ephemeral key
        KeyPair keyPair = Secure.generateECKeyPair();
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        byte[] publicKeyBytes = Secure.getPublicKeyBytes(publicKey);
        // tool sends Open Secure Channel Command APDU with public key
        // TODO make method for this
        ResponseAPDU openSCResponse = cardMngr.transmit(buildAPDU(0x42, publicKeyBytes));
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
        byte[] sharedSecrets = Secure.computeSharedSecrets(cardPublicKey, privateKey, pairingSecret, salt);
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
    // TODO change to match decomposision of code - is it good to return CommandAPDU?
    public static CommandAPDU prepareSecureAPDU(byte CLA, byte INS, byte[] data, byte[] iv, byte[] key, byte[] macKey)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        byte[] apduBuffer = null;
        short filledLength = 0;
        if (data != null) {
            // no data part in APDU
            byte[] encryptedPayload = Secure.aesEncrypt(data, iv, key);
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
        Secure.computeMacAndAppend(apduBuffer, filledLength, macKey, iv);
        // set iv for next decryption
        Secure.setIV(iv, apduBuffer, (short) (apduBuffer.length - 16));
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
        boolean verified = Secure.verifyResponseMAC(macKey, responseData);
        if (!verified) {
            System.out.println("MAC not verified!");
            return null;
        } else {
            System.out.println("MAC verified!");
        }
        // decrypt payload
        // TODO: not working decryption
        byte[] decrypted = Secure.aesDecrypt(responseData, encryptionKey, iv);
        if (decrypted.length > 0) {
            System.out.println("Decrypted payload!");
        }
        // set MAC as new iv for next encryption
        Secure.setIV(iv, responseData, (short) (responseData.length - 16));
        return decrypted;
    }
}
