package tests;

import applet.FileSystem;
import applet.Record;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.fill;

class RecordTest {

    @Test
    public void createRecord()  {
        byte[] buffer = new byte[10];
        Record record = new Record();
        try {
            byte nameLen = record.getName(buffer);
            Assertions.assertEquals(nameLen, 0);
        } catch (Exception e) {
            Assertions.fail("Getting name failed");
        }
        Assertions.assertEquals(0, record.isEmpty());
    }

    @Test
    public void initRecord()  {
        byte[] name = {1, 2, 3, 4};
        byte[] secret = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        Record record = new Record();
        try {
            record.initRecord(name, (byte) name.length, secret, (byte) secret.length);
        } catch (Exception e) {
            Assertions.fail("Record init failed");
        }
        Assertions.assertEquals(1, record.isEmpty());

        byte[] buffer = new byte[10];
        byte[] nameBuffer = new byte[4];
        try {
            short secretLen = record.getSecret(buffer);
            Assertions.assertEquals(secret.length, secretLen);
        } catch (Exception e) {
            Assertions.fail("Get secret failed");
        }
        Assertions.assertArrayEquals(buffer, secret);

        try {
            byte nameLen = record.getName(nameBuffer);
            Assertions.assertEquals(name.length, nameLen);
        } catch (Exception e) {
            Assertions.fail("Get name failed");
        }
        Assertions.assertArrayEquals(nameBuffer, name);

    }

    @Test
    public void deleteRecord()  {
        byte[] name = {1, 2, 3, 4};
        byte[] secret = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        Record record = new Record();

        try {
            record.initRecord(name, (byte) name.length, secret, (byte) secret.length);
        } catch (Exception e) {
            Assertions.fail("Record init failed");
        }
        Assertions.assertEquals(1, record.isEmpty());

        try {
            record.eraseRecord();
        } catch (Exception e) {
            Assertions.fail("Erase failed");
        }

        byte[] buffer = new byte[10];
        try {
            byte nameLen = record.getName(buffer);
            Assertions.assertEquals(nameLen, 0);
        } catch (Exception e) {
            Assertions.fail("Erase failed");
        }
        Assertions.assertEquals(0, record.isEmpty());

    }

    static class FileSystemTest {

        @Test
        void initFilesystem() {
            FileSystem fs = new FileSystem();
            byte[] buffer = new byte[255];
            Assertions.assertEquals(fs.getNumberOfRecords(), 0);

            // No names should be there
            try {
                short len = fs.getAllNames(buffer);
                Assertions.assertEquals(len, 0);
            } catch (Exception e) {
                Assertions.fail("Searching for secret failed");
            }

            // searching for name should fail
            try {
                Assertions.assertEquals(fs.getSecretByName(buffer, (byte) 5, buffer), 0);
            } catch (Exception e) {
                Assertions.fail("Searching for secret failed");
            }

        }

        @Test
        void createRecord() {
            FileSystem fs = new FileSystem();
            byte[] name = {0x31, 0x32, 0x33, 0x34};
            byte[] secret = {0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37};

            try {
                fs.createRecord(name, (byte) name.length, secret, (byte) secret.length);
            } catch (Exception e) {
                Assertions.fail("Creating record failed");
            }
            Assertions.assertEquals(fs.getNumberOfRecords(), 1);
        }

        @Test
        void deleteRecord() {
            FileSystem fs = new FileSystem();
            byte[] name = {0x31, 0x32, 0x33, 0x34};
            byte[] secret = {0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37};

            try {
                fs.createRecord(name, (byte) name.length, secret, (byte) secret.length);
            } catch (Exception e) {
                Assertions.fail("Creating record failed");
            }
            Assertions.assertEquals(1, fs.getNumberOfRecords());

            try {
                fs.deleteRecord(name, (byte) name.length);
            } catch (Exception e) {
                Assertions.fail("Creating record failed");
            }
            Assertions.assertEquals(0, fs.getNumberOfRecords());
        }

        @Test
        void getSecretByName() {
            FileSystem fs = new FileSystem();
            byte[] name = {0x31, 0x32, 0x33, 0x34};
            byte[] secret = {0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37};
            byte[] buffer = new byte[secret.length];

            try {
                fs.createRecord(name, (byte) name.length, secret, (byte) secret.length);
            } catch (Exception e) {
                Assertions.fail("Creating record failed");
            }
            Assertions.assertEquals(1, fs.getNumberOfRecords());

            try {
                fs.getSecretByName(name, (byte) name.length, buffer);
            } catch (Exception e) {
                Assertions.fail("Creating record failed");
            }
            Assertions.assertArrayEquals(secret, buffer);
            Assertions.assertEquals(1, fs.getNumberOfRecords());
        }

        @Test
        void getAllNames() {
            FileSystem fs = new FileSystem();
            byte[] name = {0x31, 0x32, 0x33, 0x34};
            byte[] secret = {0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37};
            byte[] buffer = new byte[160];
            fill(buffer, (byte) 0);
            byte[] output = new byte[160];
            fill(output, (byte) 0);
            output[0] = 0x04;
            output[1] = 0x31;
            output[2] = 0x32;
            output[3] = 0x33;
            output[4] = 0x34;

            try {
                fs.createRecord(name, (byte) name.length, secret, (byte) secret.length);
            } catch (Exception e) {
                Assertions.fail("Creating record failed");
            }
            Assertions.assertEquals(1, fs.getNumberOfRecords());

            try {
                fs.getAllNames(buffer);
            } catch (Exception e) {
                Assertions.fail("Creating record failed");
            }
            Assertions.assertArrayEquals(output, buffer);
        }
    }
}