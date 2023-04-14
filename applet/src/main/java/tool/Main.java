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

public class Main {
    private static final CommandParser cmdParser = new CommandParser();
    private static final boolean release = false;
    private static final boolean simulator = false;
    private static final String APPLET_AID = "63617264736D6172746170706C6574";
    private static final byte[] APPLET_AID_BYTE = Util.hexStringToByteArray(APPLET_AID);

    public static void main(String[] args) throws Exception {
        // create secure object implementing all SC functions, all sensitive data (keys, iv) are stored inside
        ToolSecureChannel secure = new ToolSecureChannel();
        demo(secure);
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
        initializeApplet(cardMngr, demoArguments, secure);
        openSecureChannel(cardMngr, demoArguments, secure);
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

    private static void sendAPDU(String[] cmd, ToolSecureChannel secure) throws Exception {
        CommandLine cmd_parsed = cmdParser.parse(cmdParser.options, cmd);
        Arguments args = new Arguments(cmd_parsed);

        if (!args.validateInput()) {
            return;
        }

        final CardManager cardMngr = getCardMngr();
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
        else if (cmd_parsed.hasOption('t'))
            initializeApplet(cardMngr, args, secure);
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

    private static void smartie(ToolSecureChannel secure) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("smartie$ ");
        String line;

        while (!Objects.equals((line = scanner.nextLine()), "quit")) {
            String[] cmd = line.split(" ");
            sendAPDU(cmd, secure);

            System.out.print("smartie$ ");
        }
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

    private static byte[] cardGetPublicKey(CardManager cardMngr) throws CardException {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x40, new byte[]{}));
        //TODO check response SW
        return response.getData();
    }

    private static void initializeApplet(CardManager cardMngr, Arguments args, ToolSecureChannel secure)
            throws CardException, NoSuchAlgorithmException, InvalidKeyException {
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

    private static void openSecureChannel(CardManager cardMngr, Arguments args, ToolSecureChannel secure)
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
            return;
        }
        secure.createSharedSecrets(args.pairingSecret, cardPublicKeyBytes, openSCResponse.getData());
    }
}
