package tool;

import org.apache.commons.cli.*;


public class CommandParser implements CommandLineParser {
    public Options options = new Options();
    private final CommandLineParser parser = new DefaultParser();

    public CommandParser() {
        addOptions();
    }

    private void addOptions() {
        // quit
        options.addOption(new Option("h", "help", false, "print tool usage and options"));
        options.addOption(new Option("l", "list", false, "list all names"));
        options.addOption(buildOption("v", "value", "name", "get value of secret"));
        options.addOption(buildOption("p", "pin", "pin", "use PIN"));
        options.addOption(buildOption("c", "change-pin", "pin", "change PIN"));
        options.addOption(buildOption("s", "store-secret", "name", "store secret from input file"));
        options.addOption(buildOption("i", "in-file", "file", "input file"));
        options.addOption(buildOption("d", "delete", "name", "delete secret"));
    }

    private Option buildOption(String op, String long_op, String arg_name, String description) {
        return Option.builder(op).longOpt(long_op)
                .argName(arg_name).hasArg().required(false)
                .desc(description)
                .build();
    }

    public void printHelp() {
        System.out.println("Usage for card smart command line tool:");
        System.out.println(options.toString());
    }

    private boolean validateOptionArg(CommandLine cmd, String opt) throws ParseException {
        if (cmd.hasOption(opt) &&
                (cmd.getOptionValue(opt) == null || cmd.getOptionValues(opt).length != 1)) {
            System.out.println("Command: `--" + opt + "` needs one argument!");
            return false;
        }
        return true;
    }

    private boolean validateCmd(CommandLine cmd) throws ParseException {

        if ((cmd.hasOption("help") || cmd.hasOption("list"))
                && ((cmd.getOptions().length > 1) || cmd.getArgs().length > 0)) {
            System.out.println("Command: `--" + cmd.getOptions()[0].getLongOpt()
                    + "` can't be used with another option / argument!");
            return false;
        }

        if ((cmd.hasOption("v") || cmd.hasOption("c")
                || cmd.hasOption("s") || cmd.hasOption("d"))
                && !cmd.hasOption("p")) {
            Option[] opt = cmd.getOptions();
            System.out.println("Command: `--" + opt[0].getLongOpt() 
                    + "` mused be used with `--pin` option!");
            return false;
        }

        if (!validateOptionArg(cmd, "v")
                || !validateOptionArg(cmd, "c")
                || !validateOptionArg(cmd, "s")
                || !validateOptionArg(cmd, "i")
                || !validateOptionArg(cmd, "d")
                || !validateOptionArg(cmd, "p")) {
            return false;
        }

        if ((cmd.hasOption("v") && cmd.getOptionValue("v") == null)
                || (cmd.hasOption("c") && cmd.getOptionValue("c") == null)
                || (cmd.hasOption("s") && cmd.getOptionValue("s") == null)
                || (cmd.hasOption("i") && cmd.getOptionValue("i") == null)
                || (cmd.hasOption("d") && cmd.getOptionValue("d") == null)) {
            System.out.println("Command: `--" + cmd.getOptions()[0].getLongOpt() + "` needs one argument!");
            return false;
        }

        if ((cmd.hasOption("s") && !cmd.hasOption("i"))
                || (cmd.hasOption("i") && !cmd.hasOption("s"))) {
            System.out.println("Commands: `-s` and `-i` need to be used together");
            return false;
        }

        return true;
    }

    @Override
    public CommandLine parse(Options options, String[] strings) throws ParseException {
        try {
            CommandLine cmd = parser.parse(this.options, strings);

            if (cmd.getOptions().length < 1 || !validateCmd(cmd)) {
                printHelp();
                return null;
            }
            return cmd;
        } catch (ParseException exp) {
            System.out.println("Unexpected exception: " + exp.getMessage());
        }
        return null;
    }

    @Override
    public CommandLine parse(Options options, String[] strings, boolean b) throws ParseException {
        return parse(options, strings);
    }
}