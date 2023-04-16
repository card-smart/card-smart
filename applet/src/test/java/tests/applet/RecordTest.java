package tests.applet;

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
            byte nameLen = record.getName(buffer, (short) 0);
            Assertions.assertEquals(nameLen, 0);
        } catch (Exception e) {
            Assertions.fail("Getting name failed");
        }
        Assertions.assertEquals(1, record.isEmpty());
    }

    @Test
    public void initRecord()  {
        // setSecret stores 32 bytes
        byte[] data = {4 /* name len */, 1, 2, 3, 4,
                10 /* secret len */, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        Record record = new Record();
        try {
            record.initRecord(data, (byte) 4, (short) 1, (byte) 10, (short) 6);
        } catch (Exception e) {
            Assertions.fail("Record init failed");
        }
        Assertions.assertEquals(0, record.isEmpty());

        byte[] buffer = new byte[32];
        byte[] nameBuffer = new byte[4];
        try {
            short secretLen = record.getSecret(buffer, (short) 0);
            Assertions.assertEquals(10, secretLen);
        } catch (Exception e) {
            Assertions.fail("Get secret failed");
        }
        Assertions.assertArrayEquals(buffer, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});

        try {
            byte nameLen = record.getName(nameBuffer, (short) 0);
            Assertions.assertEquals(4, nameLen);
        } catch (Exception e) {
            Assertions.fail("Get name failed");
        }
        Assertions.assertArrayEquals(nameBuffer, new byte[]{1, 2, 3, 4});

    }

    @Test
    public void deleteRecord()  {
        byte[] data = {4 /* name len */, 1, 2, 3, 4,
                10 /* secret len */, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        Record record = new Record();
        try {
            record.initRecord(data, (byte) 4, (short) 1, (byte) 10, (short) 6);
        } catch (Exception e) {
            Assertions.fail("Record init failed");
        }
        Assertions.assertEquals(0, record.isEmpty());

        try {
            record.eraseRecord();
        } catch (Exception e) {
            Assertions.fail("Erase failed");
        } 

        byte[] buffer = new byte[10];
        try {
            byte nameLen = record.getName(buffer, (short) 0);
            Assertions.assertEquals(nameLen, 0);
        } catch (Exception e) {
            Assertions.fail("Erase failed");
        }
        Assertions.assertEquals(1, record.isEmpty());

    }
}