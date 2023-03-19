package tests;

import applet.FileSystem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemTest {

    @Test
    void initFilesystem() {
        FileSystem fs = new FileSystem();
        Assertions.assertEquals(fs.getNumberOfRecords(), 0);

        byte[] buffer = new byte[160];
        try {
            short len = fs.getAllNames(buffer);
            Assertions.assertEquals(0, len);
        } catch (Exception e) {
            Assertions.fail("Getting all names failed.");
        }
    }

    @Test
    void createNewRecord() {
        FileSystem fs = new FileSystem();
        Assertions.assertEquals(fs.getNumberOfRecords(), 0);

//        byte[] name = {1, 2, 3, 4};
//        byte[] secret = {1, 2, 3, 4, 5};
        byte[] values = {4, 1, 2, 3, 4, 1, 2, 3, 4, 5};
        byte nameLength = values[0];
        byte secretLength = (byte) (values.length - nameLength - 1);
        byte[] buffer = new byte[256];
        System.arraycopy(values, 0, buffer, 0, values.length);

        try {
            fs.createRecord(buffer, nameLength, (short) 1, secretLength, (short) (1 + nameLength));
        } catch (Exception e) {
            Assertions.fail("Getting all names failed.");
        }
        Assertions.assertEquals(fs.getNumberOfRecords(), 1);
    }

    @Test
    void getNumberOfRecords() {
        FileSystem fs = new FileSystem();
        Assertions.assertEquals(fs.getNumberOfRecords(), 0);

        try {
//            byte[] name = {1, 2, 3, 4};
//            byte[] secret = {1, 2, 3, 4, 5};
            byte[] values = {4, 1, 2, 3, 4, 1, 2, 3, 4, 5};
            byte nameLength = values[0];
            byte secretLength = (byte) (values.length - nameLength - 1);
            byte[] buffer = new byte[256];
            System.arraycopy(values, 0, buffer, 0, values.length);

            fs.createRecord(buffer, nameLength, (short) 1, secretLength, (short) (1 + nameLength));
        } catch (Exception e) {
            Assertions.fail("Getting all names failed.");
        }
        try {
//            byte[] name = {2, 2, 3, 4};
//            byte[] secret = {1, 2, 3, 4, 5};
            byte[] values = {4, 2, 2, 3, 4, 1, 2, 3, 4, 5};
            byte nameLength = values[0];
            byte secretLength = (byte) (values.length - nameLength - 1);
            byte[] buffer = new byte[256];
            System.arraycopy(values, 0, buffer, 0, values.length);

            fs.createRecord(buffer, nameLength, (short) 1, secretLength, (short) (1 + nameLength));
        } catch (Exception e) {
            Assertions.fail("Getting all names failed.");
        }
        try {
//            byte[] name = {3, 2, 3, 4};
//            byte[] secret = {1, 2, 3, 4, 5};
            byte[] values = {4, 3, 2, 3, 4, 1, 2, 3, 4, 5};
            byte nameLength = values[0];
            byte secretLength = (byte) (values.length - nameLength - 1);
            byte[] buffer = new byte[256];
            System.arraycopy(values, 0, buffer, 0, values.length);

            fs.createRecord(buffer, nameLength, (short) 1, secretLength, (short) (1 + nameLength));
        } catch (Exception e) {
            Assertions.fail("Getting all names failed.");
        }
        Assertions.assertEquals(fs.getNumberOfRecords(), 3);
    }

    @Test
    void deleteRecord() {
        FileSystem fs = new FileSystem();
        Assertions.assertEquals(fs.getNumberOfRecords(), 0);

        byte[] name = {1, 2, 3, 4};
        byte[] secret = {1, 2, 3, 4, 5};
        byte[] values = {4, 1, 2, 3, 4, 1, 2, 3, 4, 5};
        byte nameLength = values[0];
        byte secretLength = (byte) (values.length - nameLength - 1);
        byte[] buffer = new byte[256];
        System.arraycopy(values, 0, buffer, 0, values.length);


        try {
            fs.createRecord(buffer, nameLength, (short) 1, secretLength, (short) (1 + nameLength));
        } catch (Exception e) {
            Assertions.fail("Getting all names failed.");
        }
        Assertions.assertEquals(fs.getNumberOfRecords(), 1);

        try {
            fs.deleteRecord(name, (byte) name.length);
        } catch (Exception e) {
            Assertions.fail("Getting all names failed.");
        }
        Assertions.assertEquals(fs.getNumberOfRecords(), 0);
    }

    @Test
    void getSecretByName() {
        FileSystem fs = new FileSystem();
        Assertions.assertEquals(fs.getNumberOfRecords(), 0);

        byte[] name = {1, 2, 3, 4};
        byte[] secret = {1, 2, 3, 4, 5};
        byte[] values = {4, 1, 2, 3, 4, 1, 2, 3, 4, 5};
        byte nameLength = values[0];
        byte secretLength = (byte) (values.length - nameLength - 1);
        byte[] buffer = new byte[256];
        System.arraycopy(values, 0, buffer, 0, values.length);
        try {
            fs.createRecord(buffer, nameLength, (short) 1, secretLength, (short) (1 + nameLength));
        } catch (Exception e) {
            Assertions.fail("Getting all names failed.");
        }
        Assertions.assertEquals(fs.getNumberOfRecords(), 1);

        try {
            byte[] buffer2 = new byte[5];
            short len = fs.getSecretByName(name, (byte)name.length, buffer2);
            Assertions.assertEquals(len, secret.length);
            Assertions.assertArrayEquals(secret, buffer2);
        } catch (Exception e) {
            Assertions.fail("Getting all names failed.");
        }
    }

    @Test
    void getAllNames() {
        FileSystem fs = new FileSystem();
        Assertions.assertEquals(fs.getNumberOfRecords(), 0);

        try {
//            byte[] name = {1, 2, 3, 4};
//            byte[] secret = {1, 2};
            byte[] values = {4, 1, 2, 3, 4, 1, 2};
            byte nameLength = values[0];
            byte secretLength = (byte) (values.length - nameLength - 1);
            byte[] buffer = new byte[256];
            System.arraycopy(values, 0, buffer, 0, values.length);
            fs.createRecord(buffer, nameLength, (short) 1, secretLength, (short) (1 + nameLength));
        } catch (Exception e) {
            Assertions.fail("Getting all names failed.");
        }
        try {
//            byte[] name = {2, 2, 3, 4};
//            byte[] secret = {4, 5};
            byte[] values = {4, 2, 2, 3, 4, 4, 5};
            byte nameLength = values[0];
            byte secretLength = (byte) (values.length - nameLength - 1);
            byte[] buffer = new byte[256];
            System.arraycopy(values, 0, buffer, 0, values.length);
            fs.createRecord(buffer, nameLength, (short) 1, secretLength, (short) (1 + nameLength));
        } catch (Exception e) {
            Assertions.fail("Getting all names failed.");
        }

        try {
            byte[] buffer = new byte[160];
            byte[] output = new byte[160];
            output[0] = 4;
            output[1] = 1; output[2] = 2; output[3] = 3; output[4] = 4;
            output[5] = 4;
            output[6] = 2; output[7] = 2; output[8] = 3; output[9] = 4;
            short len = fs.getAllNames(buffer);
            Assertions.assertEquals(10, len);
            Assertions.assertArrayEquals(buffer, output);
        } catch (Exception e) {
            Assertions.fail("Getting all names failed.");
        }
    }
}