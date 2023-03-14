package tests;

import applet.Record;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

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
            byte secretLen = record.getSecret(buffer);
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

}