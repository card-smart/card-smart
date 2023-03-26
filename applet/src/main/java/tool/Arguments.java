package tool;

import org.apache.commons.cli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class Arguments {
    CommandLine cmd;
    public byte[] PIN;
    public byte[] secretName;
    public byte[] secretValue;
    public String secretFile;
    public byte[] newPIN;
    public boolean loginNeeded = true;

    public Arguments(CommandLine cmd) {
        this.cmd = cmd;

        if (cmd.hasOption('p')) {
            PIN = cmd.getOptionValue('p').getBytes();
        }
        if (cmd.hasOption('c')) {
            newPIN = cmd.getOptionValue('c').getBytes();
        }
        if (cmd.hasOption('s')) {
            secretName = cmd.getOptionValue('s').getBytes();
        }
        if (cmd.hasOption('v')) {
            secretName = cmd.getOptionValue('v').getBytes();
        }
        if (cmd.hasOption('d')) {
            secretName = cmd.getOptionValue('d').getBytes();
        }
        if (cmd.hasOption('i')) {
            secretFile = cmd.getOptionValue('i');
        }
    }

    public byte[] padPIN() {
        return Arrays.copyOf(PIN, 10);
    }

    public boolean validateInput() throws IOException {
        // here we want to validate length of given inputs and stuff

        if (cmd.hasOption('i')) {
            try {
                secretValue = Files.readAllBytes(Paths.get(cmd.getOptionValue('i')));
            } catch (IOException e) {
                return false;
            }
            if (secretValue.length < 1) {
                return false;
            }
        }

        if (PIN.length > 10) {
            return false;
        }
        PIN = padPIN();

        return true;
    }
}
