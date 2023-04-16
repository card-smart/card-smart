package tool;

import applet.CardSmartApplet;
import cardTools.CardManager;
import cardTools.RunConfig;
import cardTools.Util;
import org.apache.commons.cli.CommandLine;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

public class Main {
    private static final CommandParser cmdParser = new CommandParser();
    public static boolean simulator = false;
    private static boolean secureCommunication = false;
    private static ToolSecureChannel secure = null; // secure object implementing all SC functions,
                                                    // all sensitive data (keys, iv) are stored inside
    private static final String APPLET_AID = "63617264736D6172746170706C6574";
    private static final byte[] APPLET_AID_BYTE = Util.hexStringToByteArray(APPLET_AID);

    public static void main(String[] args) {
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
            final CardManager cardMngr = getCardMngr();
            if (cardMngr == null)
                return;
            try {
                processCommand(args, cardMngr);
            } catch (Exception e) {
                System.err.println("Processing card command failed");
            }
            return;
        }

        smartie();
    }

    private static void smartie() {
        Scanner scanner = new Scanner(System.in);
        String line;
        final CardManager cardMngr = getCardMngr();

        if (cardMngr == null)
            return;
        System.out.print("smartie$ ");
        System.out.flush();

        while (!Objects.equals((line = scanner.nextLine()), "quit")) {
            String[] cmd = line.split(" ");

            try {
                processCommand(cmd, cardMngr);
            } catch (Exception e) {
                System.err.println("Processing card command failed");
            }


            System.out.print("smartie$ ");
            System.out.flush();
        }
        System.out.println("Thank you for using smartie, " +
                "your friend for smart-card interaction.");
    }

    private static void processCommand(String[] cmd, CardManager cardMngr) throws CardException {
        if (cardMngr == null) {
            System.err.println("Card connection failed. Aborting program!");
            return;
        }

        CommandLine cmd_parsed = cmdParser.parse(cmdParser.options, cmd);
        if (cmd_parsed == null)
            return;

        Arguments args = new Arguments(cmd_parsed);
        if (!args.validateInput())
            return;

        cardMngr.setbDebug(args.debug);

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

    private static boolean checkPIN(Arguments args, CardManager cardMngr) throws CardException {
        if (!args.loginNeeded)
            return true;

        if (!cardGetPINTries(cardMngr)) {
            return false;
        }
        return cardVerifyPINOnly(cardMngr, args);
    }

    private static int checkSecureCommunication(Arguments args, CardManager cardMngr) throws CardException {
        if (secureCommunication && args.pairingSecret != null)
            System.out.println("You do not need to provide the pairing secret" +
                    " for this session anymore");

        if (secureCommunication || args.pairingSecret == null)
            return 0;

        try {
            secure = new ToolSecureChannel();
        } catch (Exception e) {
            System.out.println("We are sorry, but your HW does not support the required" +
                        " security algorithms or correct version of them");
            return 1;
        }

        if (args.init && !initializeApplet(cardMngr, args))
            return 1;

        if (!openSecureChannel(cardMngr, args))
            return 1;

        secureCommunication = true;

        if (args.init) // we do not want to continue now, command was processed
            return 1;

        return 0;
    }

    /**
     * Returns CardManager instance of real or simulated card.
     * Returned instance type depends on Main.simulator variable.
     * Returned instance is already connected to the card and SELECT APDU was sent.
     */
    private static CardManager getCardMngr() {
        CardManager cardMngr = new CardManager(false, APPLET_AID_BYTE);
        final RunConfig runCfg = RunConfig.getDefaultConfig();
        if (simulator) { // TODO or release?
            runCfg.setAppletToSimulate(CardSmartApplet.class);
            runCfg.setTestCardType(RunConfig.CARD_TYPE.JCARDSIMLOCAL); // Use simulator
        } else {
            runCfg.setTestCardType(RunConfig.CARD_TYPE.PHYSICAL); // Use real card
        }

        // Connect to first available card
        // NOTE: selects target applet based on AID specified in CardManager constructor
        System.out.print("Connecting to card...");
        try {
            if (!cardMngr.Connect(runCfg)) {
                System.out.println(" Failed.");
                return null;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println(" Done.");
        return cardMngr;
    }

    private static CommandAPDU buildAPDU(int ins, byte[] data) {
        if (secureCommunication)
            return secure.prepareSecureAPDU((byte) 0xB0, (byte) (ins + 0x10), data);
        return new CommandAPDU(0xB0, ins, 0x00, 0x00, data);
    }

    private static byte[] processResponse(ResponseAPDU response) {
        if (response.getSW() != 0x9000) {
            return responseError(response.getSW());
        }

        if (!secureCommunication)
            return response.getData();

        byte[] res = secure.getResponseData(response.getData());
        int len = res.length;

        if (res[len - 2] != (byte) 0x90) {
            int sw = (res[len - 2] << 8) + res[len - 1];
            return responseError(sw);
        }
        return Arrays.copyOf(res, len - 2);
    }

    private static byte[] responseError(int sw) {
        try {
            processSW(sw);
        } catch (CardWrongStateException | CardErrorException e) {
            System.out.println("Card error: " + e.getMessage());
            System.out.flush();
        }
        return null;
    }

    private static void printNames(byte[] names) {
        if (names == null || names.length == 0) {
            return;
        }
        byte offset = 0;
        while (true) {
            byte length = names[offset];
            if (length == 0 || names.length - (offset + 1) < length) {
                return;
            }
            for (int i = offset + 1; i < offset + 1 + length; i++) {
                System.out.printf("%02X", names[i]);
            }
            System.out.println();
            offset += length + 1;
            if (offset >= names.length) {
                return;
            }
        }
    }

    private static void cardGetNames(CardManager cardMngr) throws CardException {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x20, new byte[]{}));
        byte[] res = processResponse(response);

        if (res != null) {
            printNames(res);
        }
    }

    private static boolean cardGetPINTries(CardManager cardMngr) throws CardException {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x21, new byte[]{}));
        byte[] res = processResponse(response);
        return res != null && res[0] > (byte) 0x00;
    }

    private static boolean cardVerifyPINOnly(CardManager cardMngr, Arguments args) throws CardException {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x22, args.PIN));
        return processResponse(response) != null;
    }

    private static void cardVerifyPIN(CardManager cardMngr, Arguments args) throws CardException {
        if (!cardVerifyPINOnly(cardMngr, args)) {
            return;
        }
        System.out.println("PIN verified");
    }

    private static void cardChangePIN(CardManager cardMngr, Arguments args) throws CardException {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x23, args.newPIN));
        if (processResponse(response) != null)
            System.out.println("PIN changed");
    }

    private static void cardGetSecret(CardManager cardMngr, Arguments args) throws CardException {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x24, args.secretName));
        byte[] res = processResponse(response);
        if (res != null)
            System.out.println(Util.toHex(res));
    }

    private static void cardStoreSecret(CardManager cardMngr, Arguments args) throws CardException {
        byte[] r = Arguments.concat(new byte[]{(byte) args.secretName.length}, args.secretName,
                new byte[]{(byte) args.secretValue.length}, args.secretValue);
        byte[] data = Arrays.copyOf(r, 44);

        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x25, data));
        if (processResponse(response) != null)
            System.out.println("Secret stored");
    }

    private static void cardDeleteSecret(CardManager cardMngr, Arguments args) throws CardException {
        ResponseAPDU response = cardMngr.transmit(buildAPDU( 0x26, args.secretName));
        if (processResponse(response) != null)
            System.out.println("Secret deleted");
    }

    private static byte[] cardGetPublicKey(CardManager cardMngr) throws CardException {
        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x40, new byte[]{}));
        return processResponse(response);
    }

    private static boolean initializeApplet(CardManager cardMngr, Arguments args) throws CardException {
        byte[] cardPublicKeyBytes = cardGetPublicKey(cardMngr);
        if (cardPublicKeyBytes == null) {
            System.err.println("Failed to init applet");
            return false;
        }
        // create payload for APDU: publicKey [65 B] | IV [16 B] | encrypted [48 B]
        byte[] payload = secure.prepareInitializationPayload(cardPublicKeyBytes, args.PIN, args.pairingSecret);
        if (payload == null) {
            System.err.println("Failed to init applet");
            return false;
        }

        // init command APDU: 0xB0 | 0x41 | 0x00 | 0x00 | 0x81 | publicKey [65 B] | IV [16 B] | encrypted [48 B]
        ResponseAPDU initResponse = cardMngr.transmit(buildAPDU(0x41, payload));
        if (initResponse.getSW() != 0x9000) {
            System.err.println("Failed to init applet");
            return false;
        }
        System.out.println("Applet initialized");
        return true;
    }

    private static boolean openSecureChannel(CardManager cardMngr, Arguments args) throws CardException {
        // get public key from card
        ResponseAPDU getKeyResponse = cardMngr.transmit(buildAPDU(0x40, new byte[]{}));
        byte[] cardPublicKeyBytes = getKeyResponse.getData();
        // tool generates ephemeral key
        byte[] publicKeyBytes = secure.getFreshPublicKeyBytes();
        if (publicKeyBytes == null)
            return false;

        ResponseAPDU openSCResponse = cardMngr.transmit(buildAPDU(0x42, publicKeyBytes));
        if (openSCResponse.getSW() != 0x9000) {
            System.err.println("Failed to open SC");
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
                secure = null;
                secureCommunication = false;
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
}
