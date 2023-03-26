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
        byte[] data = Arrays.copyOf(r, 76);

        ResponseAPDU response = cardMngr.transmit(buildAPDU(0x25, data));
        System.out.println(response);
    }

    private static void cardDeleteSecret(CardManager cardMngr, Arguments args) throws Exception {
        ResponseAPDU response = cardMngr.transmit(buildAPDU( 0x26, args.secretName));
        System.out.println(response);
    }
}
