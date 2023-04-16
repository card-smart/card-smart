package tests.tool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class TestUtils {
    public static ByteArrayOutputStream getOutput() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        return out;
    }

    public static void setInput(String input) {
        InputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.US_ASCII));
        System.setIn(in);
    }
}
