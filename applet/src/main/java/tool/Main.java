package tool;

import cardTools.CardManager;
import cardTools.RunConfig;
import cardTools.Util;
import jdk.nashorn.internal.codegen.CompilerConstants;
import main.Run;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.IOException;
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

    private static void sendAPDU(String[] cmd) throws Exception {
        CommandLine cmd_parsed = cmdParser.parse(cmdParser.options, cmd);
        Callback cb = new Callback(cmd_parsed);

        if (!cb.validateInput()) {
            return;
        }

        if (simulator) {
            run.getTries();

            if (cb.loginNeeded) {
                //run.login(cb);
            }
            if (cmd_parsed.hasOption('c')) {
                //run.changePIN(cb);
            }

            return;
        }

        if (cb.loginNeeded) {
            //login();
        }

        final CardManager cardMngr = new CardManager(true, APPLET_AID_BYTE);
        final RunConfig runCfg = RunConfig.getDefaultConfig();
        runCfg.setTestCardType(RunConfig.CARD_TYPE.PHYSICAL); // Use real card
        // Connect to first available card
        // NOTE: selects target applet based on AID specified in CardManager constructor
        System.out.print("Connecting to card...");
        if (!cardMngr.Connect(runCfg)) {
            System.out.println(" Failed.");
            return;
        }
        System.out.println(" Done.");
        final ResponseAPDU response = cardMngr.transmit(new CommandAPDU(0xB0, 0x60, 0x00, 0x00));
        byte[] data = response.getData();
        System.out.println(response);
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
}