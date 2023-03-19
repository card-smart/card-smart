package applet;

import javacard.framework.JCSystem;
import javacard.framework.Util;

import static applet.CardSmartApplet.NAME_MAX_LEN;
import static javacard.framework.Util.arrayCompare;

public class FileSystem {

    /*
     * FileSystem constants
     */
    private static final byte RECORDS_MAX_NUMBER = (byte) 16;
    private static final short TEMP_ARRAY_LEN = (short) 256;

    /*
     * Records store in filesystem
     */
    private final Record[] records;
    private short numberOfRecords;
    private final byte[] tempArray;

    /**
     * Constructor for creating card filesystem
     */
    public FileSystem() {
        /* Allocate maximum number of records */
        records = new Record[RECORDS_MAX_NUMBER];
        for (byte i = 0; i < RECORDS_MAX_NUMBER; i++) {
            records[i] = new Record();
        }

        numberOfRecords = 0;
        /* Temporal array for storing values */
        tempArray = JCSystem.makeTransientByteArray(TEMP_ARRAY_LEN, JCSystem.CLEAR_ON_DESELECT);
    }

    /**
     * Get number of the non-empty records in the filesystem
     *
     * @return number of records in the filesystem
     */
    public short getNumberOfRecords() {
        return numberOfRecords;
    }

    /**
     * Find index of first empty record slot
     *
     * @return index of the first empty record slot in the filesystem
     */
    private byte getIndexOfFirstEmptyRecord() {
        byte index;
        for (index = 0; index < RECORDS_MAX_NUMBER; index++) {
            if (records[index].isEmpty() == 0) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Get index of record with given name
     *
     * @param name    A byte[] containing the name.
     * @param nameLength The length of the name.
     * @throw InvalidArgumentException   When name is Null.
     * @throw StorageException When name is empty ("") and length is not 0.
     */
    private byte getIndexByName(byte[] name, byte nameLength) throws InvalidArgumentException, StorageException {
        for (byte index = 0; index < RECORDS_MAX_NUMBER; index++) {
            if (records[index].isEmpty() == 0)
                continue;
            byte len = records[index].getName(tempArray);
            if (len == nameLength
                    && arrayCompare(name, (short) 0, tempArray, (short) 0, nameLength) == 0) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Store name and secret in the first empty record slot
     *
     * @param buffer    A temporary byte[] containing the name and secret.
     * @param nameLength The length of the name.
     * @param nameOffset  offset of the name in buffer
     * @param secretLength The length of the secret
     * @param secretOffset  offset of the secret in buffer
     * @throw InvalidArgumentException
     * @throw StorageException
     */
    public void createRecord(byte[] buffer, byte nameLength, short nameOffset, byte secretLength, short secretOffset) throws StorageException, InvalidArgumentException {
        if (buffer == null || nameLength <= 0 || secretLength <= 0) {
            throw new InvalidArgumentException("Invalid arguments when creating record");
        }
        if (nameLength < 4 || nameLength > 10 || secretLength > 64) {
            throw new InvalidArgumentException("Invalid name or secret length");
        }

        byte index = getIndexOfFirstEmptyRecord();
        if (index < 0) {
            throw new StorageException("Storage full");
        }
        records[index].initRecord(buffer, nameLength, nameOffset, secretLength, secretOffset);
        numberOfRecords++;
    }


    /**
     * Delete record with given name if it exists
     *
     * @param name    A byte[] containing the name.
     * @param nameLength The length of the name.
     * @throw InvalidArgumentException
     * @throw StorageException
     */
    public void deleteRecord(byte[] name, byte nameLength) throws InvalidArgumentException, StorageException {
        if (nameLength < 0) {
            throw new InvalidArgumentException("Invalid length of name");
        }

        /* Find index of given record */
        byte index = getIndexByName(name, nameLength);
        if (index < 0) {
            throw new StorageException("No record with given name exists");
        }
        records[index].eraseRecord();
        numberOfRecords--;
    }

    /**
     * Fill output buffer with value of secret
     *
     * @param name    A byte[] containing the name.
     * @param nameLength The length of the name.
     * @param outputBuffer A byte[] output buffer where the secret is stored.
     * @throw InvalidArgumentException
     * @throw StorageException
     */
    public short getSecretByName(byte[] name, byte nameLength, byte[] outputBuffer) throws InvalidArgumentException, ConsistencyException, StorageException {
        if (name == null || nameLength < 0
                || outputBuffer == null || outputBuffer.length == 0) {
            throw new InvalidArgumentException("Invalid arguments when getting secret by name");
        }

        byte index = getIndexByName(name, nameLength);
        if (index < 0) {
            return 0;
        }

        return records[index].getSecret(outputBuffer);
    }

    /**
     * Fill output buffer wth concatenated names
     *
     * @param outputBuffer    A byte[] containing the name
     * @throw InvalidArgumentException
     * @throw StorageException
     * @return length of concatenated names and their lengths
     * */
    public short getAllNames(byte[] outputBuffer) throws InvalidArgumentException, StorageException {
        if (outputBuffer.length < RECORDS_MAX_NUMBER * NAME_MAX_LEN) {
            throw new InvalidArgumentException("Output buffer should have maximal possible length.");
        }
        short offset = 0;
        for (int index = 0; index < RECORDS_MAX_NUMBER; index++) {
            if (records[index].isEmpty() == 0)
                continue;
            byte len = records[index].getName(tempArray);
            outputBuffer[offset] = len;
            Util.arrayCopyNonAtomic(this.tempArray, (short) 0, outputBuffer, (short) (offset + 1), len);
            offset += 1 + len;
        }
        return offset;
    }
}
