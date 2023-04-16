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
    public boolean init = false;
    public byte[] pairingSecret;
    public String pairingSecretFile;

    public Arguments(CommandLine cmd) {
        this.cmd = cmd;

        if (cmd.hasOption('l')) {
            loginNeeded = false;
        }
        if (cmd.hasOption('t')) {
            init = true;
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
        if (cmd.hasOption('f')) {
            pairingSecretFile = cmd.getOptionValue('f');
        }
    }

    private boolean validatePairingSecret() {
        if (!cmd.hasOption('f'))
            return true;

        try {
            pairingSecret = Files.readAllBytes(Paths.get(cmd.getOptionValue('f')));
        } catch (IOException e) {
            if (!cmd.hasOption('t')) {
                System.out.println("You need to use option '-t' or '--init' if" +
                        " you want to create the pairing secret.");
                return false;
            }
            pairingSecret = ToolSecureChannel.createPairingSecret(pairingSecretFile);
            return pairingSecret != null;
        }
        if (pairingSecret.length != 32) {
            System.out.println("Pairing secret needs to be 32 bytes long!");
            return false;
        }
        return true;
    }

    public byte[] padBytes(byte[] PIN, int len) {
        return Arrays.copyOf(PIN, len);
    }

    public boolean validateInput() {
        // here we want to validate length of given inputs and stuff
        if (cmd.hasOption('h')) //there is no future for this option
            return false;

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

        if (!validatePairingSecret())
            return false;

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
