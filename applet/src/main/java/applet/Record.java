package applet;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.*;

import static applet.CardSmartApplet.*;

public class Record {
    private final byte[] name;
    private byte nameLength; // 4-10 bytes
    private final AESKey secret;
    private byte secretLength; // max 32 bytes
    private final Checksum checksum;
    private final byte[] crc;
    private final byte[] tempArray;
    private static final byte CRC_LEN = (byte) 4;

    /**
     * Create empty container for secret name and data
     */
    public Record() {
        /* Prepare empty name of secret */
        this.name = new byte[NAME_MAX_LEN];
        this.nameLength = 0;

        /* Prepare empty secret */
        byte[] initSecret = JCSystem.makeTransientByteArray(SECRET_MAX_LEN, JCSystem.CLEAR_ON_DESELECT);
        Util.arrayFillNonAtomic(initSecret, (short)0, SECRET_MAX_LEN, (byte)0);

        this.secret = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_256, false);
        this.secret.setKey(initSecret, (short) 0);
        this.secretLength = 0;

        /* Initialize checksum */
        this.checksum = Checksum.getInstance(Checksum.ALG_ISO3309_CRC32, false);
        this.crc = new byte[CRC_LEN];
        this.tempArray = JCSystem.makeTransientByteArray(CRC_LEN, JCSystem.CLEAR_ON_DESELECT);
    }

    /**
     * This method sets the name of an object, based on a byte array and its length
     *
     * @param buffer    A temporary byte[] containing the name and other things
     * @param nameOffset  offset of name in buffer
     * @param nameLength  length of the name
     * @throw InvalidArgumentException
     * @throw StorageException
     * */
    private void setName(byte[] buffer, short nameOffset, byte nameLength) throws InvalidArgumentException {
        if (nameLength == 0) {
            throw new InvalidArgumentException();
        }
        if (nameLength < NAME_MIN_LEN || nameLength > NAME_MAX_LEN) {
            throw new InvalidArgumentException();
        }
        Util.arrayCopyNonAtomic(buffer, nameOffset, this.name, (byte) 0, nameLength);
        this.nameLength = nameLength;
    }

    /**
     * A public method to get the name stored in a Record by copying it to the provided buffer.
     *
     * @param outputBuffer    A byte[] for storing the name
     * @throw InvalidArgumentException
     * @throw StorageException
     * @return length of the name
     * */
    public byte getName(byte[] outputBuffer) throws InvalidArgumentException, StorageException {
        if (outputBuffer.length < this.nameLength) {
            throw new InvalidArgumentException();
        }
        try {
            Util.arrayCopyNonAtomic(name, (short) 0, outputBuffer, (byte) 0, this.nameLength);
        } catch (Exception e) {
            throw new StorageException();
        }

        return this.nameLength;
    }

    /**
     * Clear previous secret, adds new one and set its name
     *
     * @param buffer    A temporary byte[] containing the secret and other things
     * @param secretOffset  offset of the secret in buffer
     * @param secretLength    Length of the secret
     * @throw InvalidArgumentException
     * @throw StorageException
     * */
    private void setSecret(byte[] buffer, short secretOffset, byte secretLength) throws InvalidArgumentException, StorageException {
        if (secretLength < SECRET_MIN_LEN || secretLength > SECRET_MAX_LEN) {
            throw new InvalidArgumentException();
        }
        try {
            Util.arrayFillNonAtomic(buffer, (short)(secretOffset + secretLength), (short)(SECRET_MAX_LEN - secretLength), (byte)0);
            this.secret.clearKey();
            this.secret.setKey(buffer, secretOffset);
            this.secretLength = secretLength;
        } catch (Exception e) {
            throw new StorageException();
        }
    }

    /**
     * Get value of secret
     * The output buffer needs to have sufficient length
     *
     * @param outputBuffer    A byte[] for secret
     * @throw InvalidArgumentException
     * @throw StorageException
     * @return length of concatenated names and their lengths
     * */
    public short getSecret(byte[] outputBuffer, short outputOffset) throws InvalidArgumentException, ConsistencyException {
        if (outputBuffer.length < SECRET_MIN_LEN || outputBuffer.length > SECRET_MAX_LEN) {
            throw new InvalidArgumentException();
        }

        /* Get value of secret */
        this.secret.getKey(outputBuffer, outputOffset);

        /* Compute checksum of secret */
        checksum.doFinal(outputBuffer, outputOffset, this.secretLength, tempArray, (short) 0);
        if (Util.arrayCompare(crc, (short) 0, tempArray, (short) 0, CRC_LEN) != 0) {
            throw new ConsistencyException();
        }

        return this.secretLength;
    }

    /**
     * Initialize record value and checksum
     *
     * @param buffer    A temporary byte[] containing the name and secret
     * @param nameOffset  offset of the name in buffer
     * @param nameLength Length of the name
     * @param secretLength Length of the secret
     * @param secretOffset  offset of the secret in buffer
     * @throw InvalidArgumentException
     * @throw StorageException
     * */
    public void initRecord(byte[] buffer, byte nameLength, short nameOffset, byte secretLength, short secretOffset) throws InvalidArgumentException, StorageException {
        this.setName(buffer, nameOffset, nameLength);
        this.setSecret(buffer, secretOffset, secretLength);
        checksum.doFinal(buffer, secretOffset, secretLength, crc, (short) 0);
    }

    /**
     * Erase record value, name and checksum
     *
     * @throw InvalidArgumentException
     * @throw StorageException
     * */
    public void eraseRecord() throws StorageException {
        try {
            this.secret.clearKey();
        } catch (Exception e) {
            throw new StorageException();
        }
        nameLength = 0;
        Util.arrayFillNonAtomic(name, (short) 0, (short) name.length, (byte) 0);
        Util.arrayFillNonAtomic(crc, (short) 0, (short) crc.length, (byte) 0);
        Util.arrayFillNonAtomic(tempArray, (short) 0, (short) tempArray.length, (byte) 0);
    }

    /**
     * Fill output buffer wth concatenated names
     *
     * @return 0 if empty, 1 otherwise
     * */
    public byte isEmpty() {
        return (byte) (nameLength > 0 ? 0 : 1);
    }
}
