package tests.tool;

import org.junit.jupiter.api.Test;
import tool.Main;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MainTest {

    private static ByteArrayOutputStream getOutput() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        return out;
    }

    @Test
    public void help() {
        ByteArrayOutputStream out = getOutput();
        String[] args = {"-h"};
        Main.simulator = true;
        Main.main(args);
        assertTrue(out.toString().length() > 0);
    }

    @Test
    public void verifyDefaultPIN() {
        ByteArrayOutputStream out = getOutput();
        String[] args = {"-p", "0000"};
        String sep = System.getProperty("line.separator");
        Main.simulator = true;
        Main.main(args);
        assertEquals("Connecting to card... Done." + sep + "PIN verified" + sep, out.toString());
    }

    @Test
    public void changePIN() {
        ByteArrayOutputStream out = getOutput();
        String[] args = {"-p", "0000", "-c", "1234"};
        String sep = System.getProperty("line.separator");
        Main.simulator = true;
        Main.main(args);
        assertEquals("Connecting to card... Done." + sep + "PIN changed" + sep, out.toString());
    }

    @Test
    public void listEmpty() {
        ByteArrayOutputStream out = getOutput();
        String[] args = {"--list"};
        String sep = System.getProperty("line.separator");
        Main.simulator = true;
        Main.main(args);
        assertEquals("Connecting to card... Done." + sep, out.toString());
    }
}
