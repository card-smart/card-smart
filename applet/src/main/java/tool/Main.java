package tool;

import applet.CardSmartApplet;
import cardTools.CardManager;
import cardTools.RunConfig;
import cardTools.Util;
import org.apache.commons.cli.CommandLine;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

import static javax.xml.bind.DatatypeConverter.printHexBinary;

public class Main {
    private static final CommandParser cmdParser = new CommandParser();
    private static final boolean simulator = false;
    private static boolean secureCommunication = false;
    private static ToolSecureChannel secure = null; // secure object implementing all SC functions,
                                                    // all sensitive data (keys, iv) are stored inside
    private static final String APPLET_AID = "63617264736D6172746170706C6574";
    private static final byte[] APPLET_AID_BYTE = Util.hexStringToByteArray(APPLET_AID);

    public static void main(String[] args) throws Exception {
        // THIS CODE IS ONLY FOR DEMO:
        //try {
        //    secure = new ToolSecureChannel();
        //} catch (Exception e) {
        //    System.out.println("We are sorry, but your HW does not support the required" +
        //            "security algorithms or correct version of them");
        //    return;
        //}
        //demo(secure);

        // RELEASE
        if (args.length > 0) {
            processCommand(args);
            return;
        }

        smartie();
    }

    private static void smartie() throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("smartie$ ");
        String line;

        while (!Objects.equals((line = scanner.nextLine()), "quit")) {
            String[] cmd = line.split(" ");
            processCommand(cmd);

            System.out.print("smartie$ ");
        }
        System.out.println("Thank you for using smartie, your friend for smart-card interaction.");
    }

    private static void processCommand(String[] cmd) throws Exception {
        CommandLine cmd_parsed = cmdParser.parse(cmdParser.options, cmd);
        Arguments args = new Arguments(cmd_parsed);
        if (!args.validateInput())
            return;

        final CardManager cardMngr = getCardMngr();
        if (cardMngr == null)
            return;

        if (checkSecureCommunication(args, cardMngr) != 0)
            return;

        if (!checkPIN(args, cardMngr))
            return;

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
        else if (cmd_parsed.hasOption('p')) // this option has to be always the last
            cardVerifyPIN(cardMngr, args);
    }

    private static boolean checkPIN(Arguments args, CardManager cardMngr) throws Exception {
        if (!args.loginNeeded)
            return true;

        if (!cardGetPINTries(cardMngr)) {
            System.out.println("You exceeded the possible tries for PIN, card is blocked");
            return false;
        }
        if (!cardVerifyPINOnly(cardMngr, args)) {
            return false;
        }
        return true;
    }

    private static int checkSecureCommunication(Arguments args, CardManager cardMngr) throws CardException, CardWrongStateException, CardErrorException {
        if (secureCommunication && args.pairingSecret != null)
            System.out.println("You do not need to provide the pairing secret for this session anymore");

        if (secureCommunication || args.pairingSecret == null)
            return 0;

        try {
            secure = new ToolSecureChannel();
        } catch (Exception e) {
            System.out.println("We are sorry, but your HW does not support the required" +
                        "security algorithms or correct version of them");
            return 1;
        }

        try {
            if (args.init)
                initializeApplet(cardMngr, args);

            if (!openSecureChannel(cardMngr, args))
                return 1;
        } catch (InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            System.out.println("HW requirements were not satisfied");
        }
        secureCommunication = true;

        return 0;
    }

    /**
     * Returns CardManager instance of real or simulated card.
     * Returned instance type depends on Main.simulator variable.
     * Returned instance is already connected to the card and SELECT APDU was sent.
     */
    private static CardManager getCardMngr() throws Exception {
        CardManager cardMngr = new CardManager(true, APPLET_AID_BYTE);
        final RunConfig runCfg = RunConfig.getDefaultConfig();
        if (simulator) { // TODO or release?
            runCfg.setAppletToSimulate(CardSmartApplet.class);
            runCfg.setTestCardType(RunConfig.CARD_TYPE.JCARDSIMLOCAL); // Use simulator
            System.out.println("USING SIMULATOR");
        } else {
            runCfg.setTestCardType(RunConfig.CARD_TYPE.PHYSICAL); // Use real card
        }

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
        if (secureCommunication)
            return secure.prepareSecureAPDU((byte) 0xB0, (byte) (ins + 0x10), data);
        return new CommandAPDU(0xB0, ins, 0x00, 0x00, data);
    }

    private static byte[] processResponse(ResponseAPDU response) throws CardWrongStateException, CardErrorException {
        if (secureCommunication) {
            byte[] res = secure.getResponseData(response.getData());
            int len = res.length;

            if (res[len - 2] != (byte) 0x90) {
                int sw = (res[len - 2] << 8) + res[len - 1];
                processSW(sw);
                return null;
            }
            return Arrays.copyOf(res, len - 2);
        }

        if (response.getSW() != 0x9000) {
            processSW(response.getSW());
            return null;
        }

        return response.getData();
    }

    private static void cardGetNames(CardManager cardMngr) throws Exception {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x20, new byte[]{}));
        byte[] res = processResponse(response);

        if (res != null) {
            printHexBinary(res);
        }
    }

    private static boolean cardGetPINTries(CardManager cardMngr) throws Exception {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x21, new byte[]{}));
        System.out.println(response);
        byte[] res = processResponse(response);
        return res != null && res[0] > (byte) 0x00;
    }

    private static boolean cardVerifyPINOnly(CardManager cardMngr, Arguments args) throws CardException, CardWrongStateException, CardErrorException {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x22, args.PIN));
        if (response.getSW() != 0x9000) {
            processSW(response.getSW());
            return false;
        }
        return true;
    }

    private static void cardVerifyPIN(CardManager cardMngr, Arguments args) throws Exception {
        if (!cardVerifyPINOnly(cardMngr, args)) {
            return;
        }
        System.out.print("Verification successful!\n");
    }

    private static void cardChangePIN(CardManager cardMngr, Arguments args) throws Exception {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x23, args.PIN));
        processResponse(response);
        System.out.print("Change PIN successful!\n");
    }

    private static void cardGetSecret(CardManager cardMngr, Arguments args) throws Exception {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x24, args.secretName));
        byte[] res = processResponse(response);
        printHexBinary(res);
    }

    private static void cardStoreSecret(CardManager cardMngr, Arguments args) throws Exception {
        byte[] r = Arguments.concat(new byte[]{(byte) args.secretName.length}, args.secretName,
                new byte[]{(byte) args.secretValue.length}, args.secretValue);
        byte[] data = Arrays.copyOf(r, 44);

        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x25, data));
        processResponse(response);
        System.out.print("Store secret successful!\n");
    }

    private static void cardDeleteSecret(CardManager cardMngr, Arguments args) throws Exception {
        ResponseAPDU response = cardMngr.transmit(buildAPDU( 0x26, args.secretName));
        processResponse(response);
        System.out.print("Delete secret successful!\n");
    }

    private static byte[] cardGetPublicKey(CardManager cardMngr) throws CardException, CardWrongStateException, CardErrorException {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x40, new byte[]{}));
        processResponse(response);
        return response.getData();
    }

    private static void initializeApplet(CardManager cardMngr, Arguments args)
            throws CardException, NoSuchAlgorithmException, InvalidKeyException, CardWrongStateException, CardErrorException {
        byte[] cardPublicKeyBytes = cardGetPublicKey(cardMngr);
        // create payload for APDU: publicKey [65 B] | IV [16 B] | encrypted [48 B]
        byte[] payload = secure.prepareInitializationPayload(cardPublicKeyBytes, args.PIN, args.pairingSecret);

        // init command APDU: 0xB0 | 0x41 | 0x00 | 0x00 | 0x81 | publicKey [65 B] | IV [16 B] | encrypted [48 B]
        ResponseAPDU initResponse = cardMngr.transmit(buildAPDU(0x41, payload));
        if (initResponse.getSW() == 0x9000)
            System.out.println("Success to init applet!");
        else {
            System.out.println("Failed to init applet!");
        }
    }

    private static boolean openSecureChannel(CardManager cardMngr, Arguments args)
            throws CardException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException {
        // get public key from card
        ResponseAPDU getKeyResponse = cardMngr.transmit(buildAPDU(0x40, new byte[]{}));
        byte[] cardPublicKeyBytes = getKeyResponse.getData();
        // tool generates ephemeral key
        byte[] publicKeyBytes = secure.getFreshPublicKeyBytes();
        ResponseAPDU openSCResponse = cardMngr.transmit(buildAPDU(0x42, publicKeyBytes));
        if (openSCResponse.getSW() == 0x9000)
            System.out.println("Success to open SC!");
        else {
            System.out.println("Failed to open SC!");
            return false;
        }
        secure.createSharedSecrets(args.pairingSecret, cardPublicKeyBytes, openSCResponse.getData());
        return true;
    }

    private static void processSW(int sw) throws CardErrorException, CardWrongStateException {
        final short RES_SUCCESS = (short)0x9000;
        /* Secure channel */
        final short RES_ERR_SECURE_CHANNEL = (short)0x6A00;
        final short RES_ERR_DECRYPTION = (short)0x6A01;
        final short RES_ERR_MAC = (short)0x6A02;
        final short RES_ERR_ECDH = (short)0x6A03;
        final short RES_ERR_UNINITIALIZED = (short)0x6A04;
        final short RES_ERR_INITIALIZED = (short)0x6A05;
        final short RES_ERR_ENCRYPTION = (short)0x6A06;
        final short RES_ERR_DATA_LENGTH = (short)0x6A07;
        /* Operations */
        final short RES_ERR_GENERAL = (short)0x6B00;
        final short RES_ERR_NOT_LOGGED = (short)0x6B01;
        final short RES_ERR_RESET = (short)0x6B02;
        final short RES_ERR_PIN_POLICY = (short)0x6B03;
        final short RES_ERR_STORAGE = (short)0x6B04;
        final short RES_ERR_NAME_POLICY = (short)0x6B05;
        final short RES_ERR_SECRET_POLICY = (short)0x6B06;
        final short RES_ERR_INPUT_DATA = (short)0x6B07;
        /* Unsupported instructions */
        final short RES_UNSUPPORTED_CLA = (short)0x6C00;
        final short RES_UNSUPPORTED_INS = (short)0x6C01;

        switch (sw) {
            case RES_SUCCESS:
                return;
            case RES_ERR_SECURE_CHANNEL:
                throw new CardErrorException("Problem with opening of secure channel.");
            case RES_ERR_DECRYPTION:
                throw new CardErrorException("Problem with APDU decryption.");
            case RES_ERR_MAC:
                throw new CardErrorException("Problem with MAC.");
            case RES_ERR_ECDH:
                throw new CardErrorException("Problem with ECDH.");
            case RES_ERR_UNINITIALIZED:
                throw new CardWrongStateException("Secure operation was executed but applet is not initialized.");
            case RES_ERR_INITIALIZED:
                throw new CardWrongStateException("Unsecure operation was executed but applet is initialized.");
            case RES_ERR_ENCRYPTION:
                throw new CardErrorException("Problem with response encryption.");
            case RES_ERR_DATA_LENGTH:
                throw new CardErrorException("Wrong length of encrypted data.");
            case RES_ERR_NOT_LOGGED:
                throw new CardWrongStateException("User is not authenticated via PIN.");
            case RES_ERR_RESET:
                throw new CardWrongStateException("No remaining tries to verify PIN, card is reset.");
            case RES_ERR_PIN_POLICY:
                throw new CardErrorException("PIN policy was not satisfied.");
            case RES_ERR_STORAGE:
                throw new CardErrorException("Problem with storage.");
            case RES_ERR_NAME_POLICY:
                throw new CardErrorException("Name policy was not satisfied.");
            case RES_ERR_SECRET_POLICY:
                throw new CardErrorException("Secret policy was not satisfied.");
            case RES_ERR_INPUT_DATA:
                throw new CardErrorException("Wrong length of data at applet initialization.");
            case RES_UNSUPPORTED_CLA:
                throw new CardErrorException("Unsupported CLA byte in received APDU.");
            case RES_UNSUPPORTED_INS:
                throw new CardErrorException("Unsupported INS byte in received APDU.");
            case RES_ERR_GENERAL:
            default:
                throw new CardErrorException("Unexpected problem occurred.");
        }
    }

    private static void demo(ToolSecureChannel secure) throws Exception {
        // connect to card
        final CardManager cardMngr = getCardMngr();
        // command line arguments
        String[] cmd = new String[]{"-t", "-f", "./pairing_secret_file", "-p", "0000"};
        CommandLine cmd_parsed = cmdParser.parse(cmdParser.options, cmd);
        Arguments demoArguments = new Arguments(cmd_parsed);
        if (!demoArguments.validateInput()) {
            System.out.print("Arguments invalid!");
            return;
        }
        /* Initialize applet workflow */
        initializeApplet(cardMngr, demoArguments);
        openSecureChannel(cardMngr, demoArguments);
        /* PIN verify demo */
        CommandAPDU verifyAPDU = secure.prepareSecureAPDU((byte) 0xB0, (byte) 0x32, demoArguments.PIN);
        ResponseAPDU verifyResponse = cardMngr.transmit(verifyAPDU);
        byte[] response = secure.getResponseData(verifyResponse.getData());
        if (Arrays.equals(response, new byte[]{-112, 0})) {
            System.out.print("Verification successful!\n");
        } else {
            System.out.print("Verification not successful!\n");
            return;
        }
        /* PIN change */
        byte[] newPIN = {0x01, 0x02, 0x03, 0x04, 0, 0, 0, 0, 0, 0};
        CommandAPDU changePINAPDU = secure.prepareSecureAPDU((byte) 0xB0, (byte) 0x33, newPIN);
        ResponseAPDU changePINResponse = cardMngr.transmit(changePINAPDU);
        response = secure.getResponseData(changePINResponse.getData());
        if (Arrays.equals(response, new byte[]{-112, 0})) {
            System.out.print("Change PIN successful!\n");
        } else {
            System.out.print("Change PIN not successful!\n");
            return;
        }
        /* Store secret */
        byte[] nameAndSecret = {4, 0x31, 0x32, 0x33, 0x34, 4,0x51, 0x52, 0x53, 0x54 };
        CommandAPDU storeAPDU = secure.prepareSecureAPDU((byte) 0xB0, (byte) 0x35, nameAndSecret);
        ResponseAPDU storeResponse = cardMngr.transmit(storeAPDU);
        response = secure.getResponseData(storeResponse.getData());
        if (Arrays.equals(response, new byte[]{-112, 0})) {
            System.out.print("Store secret successful!\n");
        } else {
            System.out.print("Store secret not successful!\n");
        }
        /* Get secret */
        byte[] name = {0x31, 0x32, 0x33, 0x34};
        CommandAPDU getAPDU = secure.prepareSecureAPDU((byte) 0xB0, (byte) 0x34, name);
        ResponseAPDU getResponse = cardMngr.transmit(getAPDU);
        response = secure.getResponseData(getResponse.getData());
        if (Arrays.equals(response, new byte[]{0x51, 0x52, 0x53, 0x54, -112, 0})) {
            System.out.print("Get secret successful!\n");
        } else {
            System.out.print("Get secret not successful!\n");
        }
        /* Get all names */
        CommandAPDU getNamesAPDU = secure.prepareSecureAPDU((byte) 0xB0, (byte) 0x30, null);
        ResponseAPDU getNamesResponse = cardMngr.transmit(getNamesAPDU);
        response = secure.getResponseData(getNamesResponse.getData());
        if (Arrays.equals(response, new byte[]{4, 0x31, 0x32, 0x33, 0x34, -112, 0})) {
            System.out.print("Get all names successful!\n");
        } else {
            System.out.print("Get all names not successful!\n");
        }
        /* Delete secret */
        CommandAPDU deleteAPDU = secure.prepareSecureAPDU((byte) 0xB0, (byte) 0x36, name);
        ResponseAPDU deleteResponse = cardMngr.transmit(deleteAPDU);
        response = secure.getResponseData(deleteResponse.getData());
        if (Arrays.equals(response, new byte[]{-112, 0})) {
            System.out.print("Delete secret successful!\n");
        } else {
            System.out.print("Delete secret not successful!\n");
        }
        /* Empty get names */
        getNamesAPDU = secure.prepareSecureAPDU((byte) 0xB0, (byte) 0x30, null);
        getNamesResponse = cardMngr.transmit(getNamesAPDU);
        response = secure.getResponseData(getNamesResponse.getData());
        if (Arrays.equals(response, new byte[]{-112, 0})) {
            System.out.print("Get all names empty successful!\n");
        } else {
            System.out.print("Get all names not empty successful!\n");
        }
    }
}
