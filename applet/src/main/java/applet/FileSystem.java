package applet;

import javacard.framework.JCSystem;
import javacard.framework.Util;


public class FileSystem {

    /*
     * FileSystem constants
     */
    private static final short NAME_MAX_LEN = Record.NAME_MAX_LEN;
    private static final short NAME_MIN_LEN = Record.NAME_MIN_LEN;
    private static final short SECRET_MAX_LEN = Record.SECRET_MAX_LEN;
    private static final short SECRET_MIN_LEN = Record.SECRET_MIN_LEN;
    private static final byte RECORDS_MAX_NUMBER = (byte) 16;
    private static final short TEMP_ARRAY_LEN = NAME_MAX_LEN;

    /*
     * Records store in filesystem
     */
    private final Record[] records;
    private short numberOfRecords;
    public final byte[] tempArray;

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
        this.tempArray = JCSystem.makeTransientByteArray(TEMP_ARRAY_LEN, JCSystem.CLEAR_ON_DESELECT);
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
            if (records[index].isEmpty() == 1) {
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
     * @param nameOffset offset where name starts
     * @throw InvalidArgumentException   When name is Null.
     * @throw StorageException When name is empty ("") and length is not 0.
     */
    private byte getIndexByName(byte[] name, byte nameLength, short nameOffset)
            throws InvalidArgumentException, StorageException {
        for (byte index = 0; index < RECORDS_MAX_NUMBER; index++) {
            if (records[index].isEmpty() == 1)
                continue;
            byte len = records[index].getName(tempArray, (short) 0);
            if (len == nameLength
                    && Util.arrayCompare(name, nameOffset, tempArray, (short) 0, nameLength) == 0) {
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
    public void createRecord(byte[] buffer, byte nameLength, short nameOffset, byte secretLength, short secretOffset)
            throws StorageException, InvalidArgumentException {
        if (buffer == null || nameLength < NAME_MIN_LEN || nameLength > NAME_MAX_LEN
                || secretLength < SECRET_MIN_LEN || secretLength > SECRET_MAX_LEN) {
            throw new InvalidArgumentException();
        }

        if (getIndexByName(buffer, nameLength, nameOffset) != -1) {
            // name already exists in filesystem
            throw new StorageException();
        }

        byte index = getIndexOfFirstEmptyRecord();
        if (index < 0) {
            throw new StorageException();
        }
        records[index].initRecord(buffer, nameLength, nameOffset, secretLength, secretOffset);
        numberOfRecords++;
    }


    /**
     * Delete record with given name if it exists
     *
     * @param buffer    A byte[] containing the name.
     * @param nameLength The length of the name.
     * @param nameOffset offset where name starts
     * @throw InvalidArgumentException
     * @throw StorageException
     */
    public void deleteRecord(byte[] buffer, byte nameLength, short nameOffset)
            throws InvalidArgumentException, StorageException {
        if (nameLength < NAME_MIN_LEN || nameLength > NAME_MAX_LEN) {
            throw new InvalidArgumentException();
        }

        /* Find index of given record */
        byte index = getIndexByName(buffer, nameLength, nameOffset);
        if (index < 0) {
            throw new StorageException();
        }
        records[index].eraseRecord();
        numberOfRecords--;
    }

    /**
     * Fill output buffer with value of secret
     *
     * @param name    A byte[] containing the name.
     * @param nameLength The length of the name.
     * @param nameOffset offset where name starts
     * @param outputBuffer A byte[] output buffer where the secret is stored.
     * @throw InvalidArgumentException
     * @throw StorageException
     */
    public short getSecretByName(byte[] name, byte nameLength, short nameOffset, byte[] outputBuffer, short outputOffset)
            throws InvalidArgumentException, ConsistencyException, StorageException {
        if (name == null || nameLength < NAME_MIN_LEN || nameLength > NAME_MAX_LEN
                || outputBuffer == null || outputBuffer.length < SECRET_MIN_LEN) {
            throw new InvalidArgumentException();
        }

        byte index = getIndexByName(name, nameLength, nameOffset);
        if (index < 0) {
            return 0;
        }

        return records[index].getSecret(outputBuffer, outputOffset);
    }

    /**
     * Fill output buffer wth concatenated names
     *
     * @param outputBuffer    A byte[] containing the name
     * @throw InvalidArgumentException
     * @throw StorageException
     * @return length of concatenated names and their lengths
     * */
    public short getAllNames(byte[] outputBuffer, short outputOffset) throws InvalidArgumentException, StorageException {
        if (outputBuffer.length < RECORDS_MAX_NUMBER * NAME_MAX_LEN + RECORDS_MAX_NUMBER) {
            throw new InvalidArgumentException();
        }
        short offset = 0;
        for (short index = 0; index < RECORDS_MAX_NUMBER; index++) {
            if (records[index].isEmpty() == 1)
                continue;
            byte len = records[index].getName(outputBuffer, (short) (outputOffset + offset + 1));
            outputBuffer[(short) (outputOffset + offset)] = len;
            offset += 1 + len;
        }
        return offset;
    }

    public void eraseData() throws StorageException {
        for (short i = 0; i < RECORDS_MAX_NUMBER; i++) {
            this.records[i].eraseRecord();
        }
    }
}
