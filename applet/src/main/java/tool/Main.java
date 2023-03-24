package tool;

import main.Run;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import java.util.Objects;
import java.util.Scanner;


public class Main {
    private static final Run run = new Run();
    public static void main(String[] args) throws ParseException {
        run.main();

        CommandParser cmdParser = new CommandParser();
        boolean release = false;
        boolean simulator = true;

        /*
        This is for our testing - on simulator or on a real card - specified above,
        simulator is the default

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
            sendAPDU(cmdParser, simulator, new String[]{"-v", "meno", "--pin", "pinik"});
            //smartie(cmdParser, simulator);
            return;
        }

        // THIS IS FOR RELEASE, NOT TESTING
        if (args.length > 0) {
            CommandLine cmd_parsed = cmdParser.parse(cmdParser.options, args);
            // TODO call desired instruction
            return;
        }

        smartie(cmdParser, false);
    }

    private static void sendAPDU(CommandParser cmdParser, boolean simulator, String[] cmd) throws ParseException {
        CommandLine cmd_parsed = cmdParser.parse(cmdParser.options, cmd);

        run.getTries();
        //run.login();
        //run.changePIN();

    }

    private static void smartie(CommandParser cmdParser, boolean simulator) throws ParseException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("smartie$ ");
        String line;

        while (!Objects.equals((line = scanner.nextLine()), "quit")) {
            String[] cmd = line.split(" ");
            CommandLine cmd_parsed = cmdParser.parse(cmdParser.options, cmd);

            // TODO fill Callback based on parsed data
            // TODO send apdu


            System.out.print("smartie$ ");
        }
    }
}