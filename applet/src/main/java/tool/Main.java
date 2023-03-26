package tool;

import cardTools.CardManager;
import cardTools.RunConfig;
import cardTools.Util;
import main.Run;
import org.apache.commons.cli.CommandLine;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
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

    public static void main(String[] args) throws Exception {
        run.main();

        /*
        This is for our testing - on simulator or on a real card - specified above,
        simulator is the default and only call, as the real card is not implemented yet

        You can use either sendAPDU() function where you can specify
        command line arguments in an array to avoid writing them each time:

            sendAPDU(cmdParser, new String[]{"-v", "meno", "--pin", "pinik"});

        OR you can use smartie function to test more behaviour.
        It will run smartie app and expect input until you type 'quit':

            smartie$ -v name --pin 75436
            // some output
            smartie$ --list
            // some output
            smartie$ quit
         */
        if (!release) {
            sendAPDU(new String[]{"-v", "meno", "--pin", "pinik"});
            //smartie();
            return;
        }

        // THIS IS FOR RELEASE, NOT TESTING
        if (args.length > 0) {
            CommandLine cmd_parsed = cmdParser.parse(cmdParser.options, args);
            // TODO call desired instruction
            return;
        }

        smartie();
    }

    private static void simulator(Arguments args, CommandLine cmd) {
        run.getTries();
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
        if (cmd_parsed.hasOption('v'))
            cardGetSecret(cardMngr, args);
        if (cmd_parsed.hasOption('c'))
            cardChangePIN(cardMngr, args);
        if (cmd_parsed.hasOption('s'))
            cardStoreSecret(cardMngr, args);
        if (cmd_parsed.hasOption('d'))
            cardDeleteSecret(cardMngr, args);

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

    private static CommandAPDU buildAPDU(byte ins, byte[] data) {
        return new CommandAPDU(0xB0, ins, 0x00, 0x00, data);
    }

    private static void cardGetNames(CardManager cardMngr) throws Exception {
        ResponseAPDU response = cardMngr.transmit(new CommandAPDU(0xB0, 0x20, 0x00, 0x00));
        System.out.println(response); // TODO do something with data
    }

    private static boolean cardGetPINTries(CardManager cardMngr) throws Exception {
        ResponseAPDU response = cardMngr.transmit(new CommandAPDU(0xB0, 0x21, 0x00, 0x00));
        System.out.println(response); // TODO do something with data
        return true;
    }

    private static int cardVerifyPINOnly(CardManager cardMngr, Arguments args) throws CardException {
        ResponseAPDU response = cardMngr.transmit(new CommandAPDU(0xB0, 0x22, 0x00, 0x00, args.PIN));
        if (response.getSW() != 0x9000) {
            System.out.print("Error verify pin TODO");
            return 1;
        }
        return 0;
    }

    // this function can be removed?
    private static void cardVerifyPIN(CardManager cardMngr, Arguments args) throws Exception {
        if (cardVerifyPINOnly(cardMngr, args) != 0) {
            // TODO something here
            return;
        }
    }

    private static void cardChangePIN(CardManager cardMngr, Arguments args) throws Exception {
        if (cardVerifyPINOnly(cardMngr, args) != 0) {
            return;
        }
        // TODO get data from Options or by function parameter
        //byte[] data = {0x31, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0}; //default pin, need to be padded to 10 B
        ResponseAPDU response = cardMngr.transmit(new CommandAPDU(0xB0, 0x23, 0x00, 0x00, args.PIN));
        System.out.println(response); // TODO do something with data
    }

    private static void cardGetSecret(CardManager cardMngr, Arguments args) throws Exception {
        if (cardVerifyPINOnly(cardMngr, args) != 0) {
            return;
        }

        // get secret
        // TODO get data from Options or by function parameter
        //byte[] name = {0x31, 0x32, 0x33, 0x34};
        ResponseAPDU response = cardMngr.transmit(new CommandAPDU(0xB0, 0x24, 0x00, 0x00, args.secretName));
        System.out.println(response);
    }

    private static void cardStoreSecret(CardManager cardMngr, Arguments args) throws Exception {

        if (cardVerifyPINOnly(cardMngr, args) != 0) {
            return;
        }

        // store secret
        // TODO get data from Options or by function parameter
        byte[] r = Arguments.concat(new byte[]{(byte) args.secretName.length}, args.secretName,
                new byte[]{(byte) args.secretValue.length}, args.secretValue);
        byte[] data = Arrays.copyOf(r, 76);

        ResponseAPDU response = cardMngr.transmit(new CommandAPDU(0xB0, 0x25, 0x00, 0x00, data));
        System.out.println(response);
    }

    private static void cardDeleteSecret(CardManager cardMngr, Arguments args) throws Exception {
        if (cardVerifyPINOnly(cardMngr, args) != 0) {
            return;
        }

        // delete secret
        ResponseAPDU response = cardMngr.transmit(new CommandAPDU(0xB0, 0x26, 0x00, 0x00, args.secretName));
        System.out.println(response);
    }
}
