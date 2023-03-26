package tool;

import org.apache.commons.cli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

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

        if (cmd.hasOption('l')) {
            loginNeeded = false;
        }
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

    public byte[] padBytes(byte[] PIN, int len) {
        return Arrays.copyOf(PIN, len);
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

        if (PIN != null) {
            if (PIN.length > 10)
                return false;
            PIN = padBytes(PIN, 10);
        }

        if (newPIN != null) {
            if (newPIN.length > 10)
                return false;
            newPIN = padBytes(newPIN, 10);
        }

        if (secretName != null && secretName.length > 10) {
            return false;
        }

        if (secretValue != null) {
            if (secretValue.length > 32)
                return false;
            secretValue = padBytes(secretValue, 32);
        }

        return true;
    }

    //source: https://www.techiedelight.com/concatenate-byte-arrays-in-java/
    public static byte[] concat(byte[]... arrays) {
        int len = Arrays.stream(arrays).filter(Objects::nonNull)
                .mapToInt(s -> s.length).sum();

        byte[] result = new byte[len];
        int lengthSoFar = 0;

        for (byte[] array: arrays) {
            if (array != null) {
                System.arraycopy(array, 0, result, lengthSoFar, array.length);
                lengthSoFar += array.length;
            }
        }

        return result;
    }
}
