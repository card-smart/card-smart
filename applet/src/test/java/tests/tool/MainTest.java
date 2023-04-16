package tests.tool;

import org.junit.jupiter.api.Test;
import tool.Main;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MainTest {

    @Test
    public void help() {
        ByteArrayOutputStream out = TestUtils.getOutput();
        String[] args = {"-h"};
        Main.simulator = true;
        Main.main(args);
        assertTrue(out.toString().length() > 0);
    }

    @Test
    public void verifyDefaultPIN() {
        ByteArrayOutputStream out = TestUtils.getOutput();
        String[] args = {"-p", "0000"};
        String sep = System.getProperty("line.separator");
        Main.simulator = true;
        Main.main(args);
        assertEquals("Connecting to card... Done." + sep + "PIN verified" + sep, out.toString());
    }

    @Test
    public void changePIN() {
        ByteArrayOutputStream out = TestUtils.getOutput();
        String[] args = {"-p", "0000", "-c", "1234"};
        String sep = System.getProperty("line.separator");
        Main.simulator = true;
        Main.main(args);
        assertEquals("Connecting to card... Done." + sep + "PIN changed" + sep, out.toString());
    }

    @Test
    public void listEmpty() {
        ByteArrayOutputStream out = TestUtils.getOutput();
        String[] args = {"--list"};
        String sep = System.getProperty("line.separator");
        Main.simulator = true;
        Main.main(args);
        assertEquals("Connecting to card... Done." + sep, out.toString());
    }

    @Test
    public void storeSecret_nonexistentFile() {
        ByteArrayOutputStream out = TestUtils.getOutput();
        String[] args = {"-s", "my_secret", "-i", "nonexistent", "-p", "0000"};
        String sep = System.getProperty("line.separator");
        Main.simulator = true;
        Main.main(args);
        assertEquals("Connecting to card... Done." + sep + "Invalid file: nonexistent" + sep, out.toString());
    }

    @Test
    public void storeSecret() {
        ByteArrayOutputStream out = TestUtils.getOutput();
        String[] args = {"-s", "my_secret", "-i", "src/test/resources/secret_value.txt", "-p", "0000"};
        String sep = System.getProperty("line.separator");
        Main.simulator = true;
        Main.main(args);
        assertEquals("Connecting to card... Done." + sep + "Secret stored" + sep, out.toString());
    }

    @Test
    public void smartieVerify() {
        ByteArrayOutputStream out = TestUtils.getOutput();
        TestUtils.setInput("-p 0000\nquit");
        String sep = System.getProperty("line.separator");
        Main.simulator = true;
        Main.main(new String[]{});
        assertEquals("Connecting to card... Done." + sep + "smartie$ PIN verified"
                + sep + "smartie$ Thank you for using smartie, your friend for smart-card interaction." + sep, out.toString());
    }

    @Test
    public void smartieListNames() {
        ByteArrayOutputStream out = TestUtils.getOutput();
        TestUtils.setInput("-p 0000 -s name -i src/test/resources/secret_value.txt\n-l\nquit");
        String sep = System.getProperty("line.separator");
        Main.simulator = true;
        Main.main(new String[]{});
        assertEquals("Connecting to card... Done." + sep
                + "smartie$ Secret stored" + sep
                + "smartie$ 6E616D65" + sep
                + "smartie$ Thank you for using smartie, your friend for smart-card interaction." + sep, out.toString());
    }

    @Test
    public void smartieDelete() {
        ByteArrayOutputStream out = TestUtils.getOutput();
        TestUtils.setInput("-p 0000 -s name -i src/test/resources/secret_value.txt\n-l\n-d name -p 0000\n-l\nquit");
        String sep = System.getProperty("line.separator");
        Main.simulator = true;
        Main.main(new String[]{});
        assertEquals("Connecting to card... Done." + sep
                + "smartie$ Secret stored" + sep
                + "smartie$ 6E616D65" + sep
                + "smartie$ Secret deleted" + sep
                + "smartie$ " // output of last -l option
                + "smartie$ Thank you for using smartie, your friend for smart-card interaction." + sep, out.toString());
    }
}
