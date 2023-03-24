package tool;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import java.util.Objects;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws ParseException {
        System.out.println("Hello world!");

        //String[] cmd = {"-v", "meno"};
        //String[] cmd = {"-v", "meno", "--pin", "pinik"};
        //String[] cmd = {"-v", "--pin", "pinik"};
        //String[] cmd = {"-v", "meno", "naviac", "--pin", "pinik"};

        CommandParser cmdParser = new CommandParser();

        if (args.length > 0) {
            //CommandLine cmd_parsed = cmdParser.parse(cmdParser.options, cmd);
            CommandLine cmd_parsed = cmdParser.parse(cmdParser.options, args);

            // goto logic of the code


            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("smartie$ ");
        String line;

        while (!Objects.equals((line = scanner.nextLine()), "quit")) {
            String[] cmd = line.split(" ");

            // TODO fill Callback based on parsed data
            // TODO send apdu

            CommandLine cmd_parsed = cmdParser.parse(cmdParser.options, cmd);
            System.out.print("smartie$ ");
        }



        // Call for functionality based on the option:
        // TODO
    }
}