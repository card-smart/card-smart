package tool;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Util {

    /**
     * Write byte data into file
     * @param path path of file to be written into
     * @param data data to be written
     */
    public static void writeIntoFile(String path, byte[] data) throws IOException {
        try (FileOutputStream output = new FileOutputStream(path)) {
            output.write(data);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Read byte array from file
     * @param path path to file to be read
     * @return read byte array
     */
    public static byte[] readFromFile(String path) throws IOException {
        FileInputStream input = null;
        byte[] data = null;
        try {
            input = new FileInputStream(path);
            data = new byte[input.available()];
            input.read(data);
        } finally {
            if (input != null) {
                input.close();
            }
        }
        return data;
    }
}
