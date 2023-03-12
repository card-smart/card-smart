package applet;

import static javacard.framework.Util.arrayCompare;

public class FileSystem {

    /*
     * FileSystem constants
     */
    private static final byte RECORDS_MAX_NUMBER = (byte) 16;

    /*
     * Records store in filesystem
     */
    Record[] records;
    byte numberOfRecords;

    /*
     * Create filesystem instance
     */
    public FileSystem() {
        /* Allocate maximum number of records */
        records = new Record[RECORDS_MAX_NUMBER];
        numberOfRecords = 0;
    }

    /*
     * Find index of first empty record slot
     */
    private byte getIndexOfFirstEmptyRecord() {
        byte index;
        for (index = 0; index < RECORDS_MAX_NUMBER; index++) {
            if (records[index].isEmpty() == 1) {
                return index;
            }
        }
        return -1;
    }

    /*
     * Get index of record with given name
     */
    private byte getIndexByName(byte[] name, byte nameLen) {
        byte index = 0;
        for (index = 0; index < RECORDS_MAX_NUMBER; index++) {
            if (arrayCompare(name, (short) 0, records[index].getName(), (short) 0, nameLen) == 0) {
                return index;
            }
        }
        return -1;
    }

    /*
     * Store name and secret in the first empty record slot
     */
    public void createRecord(byte[] name, byte nameLen, byte[] secret, byte secretLen) throws StorageException, InvalidArgumentException {
        if (name == null || nameLen < 0
                || secret == null || secretLen < 0) {
            throw new InvalidArgumentException("Invalid arguments when creating record");
        }

        byte index = getIndexOfFirstEmptyRecord();
        if (index < -1) {
            throw new StorageException("Storage full");
        }
        records[index].initRecord(name, nameLen, secret, secretLen);
    }

    public void deleteRecord(byte[] name, byte nameLen) throws InvalidArgumentException, StorageException {
        if (nameLen < 0) {
            throw new InvalidArgumentException("Invalid length of name");
        }

        /* Find index of given record */
        byte index = getIndexByName(name, nameLen);
        if (index < 0) {
            throw new StorageException("No record with given name exists");
        }
        records[index].eraseRecord();
    }

    /*
     * Fill output buffer with value of secret
     */
    public byte getSecretByName(byte[] name, byte nameLen, byte[] outBuffer, byte bufferLen) throws InvalidArgumentException, ConsistencyException {
        if (name == null || nameLen < 0
                || outBuffer == null || bufferLen < 0) {
            throw new InvalidArgumentException("Invalid arguments when getting secret by name");
        }

        byte index = getIndexByName(name, nameLen);
        return records[index].getSecret(outBuffer, bufferLen);
    }

    public void getAllNames(byte[] outBuffer, byte bufferLen) {
        // TODO
    }
}
