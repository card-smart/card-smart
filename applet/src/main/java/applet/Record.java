package applet;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.Checksum;
import javacard.security.HMACKey;
import javacard.security.KeyBuilder;

import static applet.CardSmartApplet.*;
import static javacard.framework.Util.*;

public class Record {
    private final byte[] name;
    private byte nameLen;
    private final HMACKey secret;
    private final Checksum checksum;
    private final byte[] crc;
    private final byte[] tempArray;
    private static final byte CRC_LEN = (byte) 4;

    /*
     * Create empty container for secret name and data
     */
    public Record() {
        /* Prepare empty name of secret */
        name = new byte[NAME_MAX_LEN];
        nameLen = 0;

        /* Prepare empty secret */
        byte[] initSecret = JCSystem.makeTransientByteArray((short) 64, JCSystem.CLEAR_ON_DESELECT);
        secret = (HMACKey) KeyBuilder.buildKey(KeyBuilder.TYPE_HMAC, KeyBuilder.LENGTH_HMAC_SHA_256_BLOCK_64, false);
        secret.setKey(initSecret, (short) 0, SECRET_MAX_LEN);

        /* Initialize checksum */
        checksum = Checksum.getInstance(Checksum.ALG_ISO3309_CRC32, false);
        crc = new byte[CRC_LEN];
        tempArray = JCSystem.makeTransientByteArray(CRC_LEN, JCSystem.CLEAR_ON_DESELECT);
    }

    /*
     * Set new name to record
     */
    private void setName(byte[] name, byte length) throws InvalidArgumentException {
        if (length < NAME_MIN_LEN || length > NAME_MAX_LEN) {
            throw new InvalidArgumentException("Wrong length of the name.");
        }
        Util.arrayCopyNonAtomic(name, (short) 0, this.name, (byte) 0, length);
        nameLen = length;
    }

    /*
     * Get name of the record
     */
    public byte getName(byte[] buffer) throws InvalidArgumentException, StorageException {
        if (buffer.length < nameLen) {
            throw new InvalidArgumentException("Buffer too small.");
        }
        try {
            Util.arrayCopyNonAtomic(name, (short) 0, buffer, (byte) 0, nameLen);
        } catch (Exception e) {
            throw new StorageException("Cannot copy secret name.");
        }

        return nameLen;
    }

    /*
     * Clear previous secret and set its name
     */
    private void setSecret(byte[] secret, byte length) throws InvalidArgumentException, StorageException {
        if (length < SECRET_MIN_LEN || length > SECRET_MAX_LEN) {
            throw new InvalidArgumentException("Wrong length of the secret.");
        }
        try {
            this.secret.clearKey();
        } catch (Exception e) {
            throw new StorageException("Can not clear key");
        }
        this.secret.setKey(secret, (byte) 0, length);
    }

    /*
     * Get value of secret
     * The output buffer needs to have sufficient length
     */
    public byte getSecret(byte[] buffer) throws InvalidArgumentException, ConsistencyException {
        if (buffer.length < SECRET_MIN_LEN || buffer.length > SECRET_MAX_LEN) {
            throw new InvalidArgumentException("Wrong length of the buffer for secret.");
        }

        /* Get value of secret */
        byte secretLen = this.secret.getKey(buffer, (short) 0);

        /* Compute checksum of secret */
        checksum.doFinal(buffer, (short) 0, secretLen, tempArray, (short) 0);
        if (Util.arrayCompare(crc, (short) 0, tempArray, (short) 0, CRC_LEN) != 0) {
            throw new ConsistencyException("CRC does not match");
        }

        return secretLen;
    }

    /*
     * Initialize record value and checksum
     */
    public void initRecord(byte[] name, byte nameLen, byte[] secret, byte secretLen) throws InvalidArgumentException, StorageException {
        this.setName(name, nameLen);
        this.setSecret(secret, secretLen);
        checksum.doFinal(secret, (short) 0, secretLen, crc, (short) 0);
    }

    /*
     * Erase record value, name and checksum
     */
    public void eraseRecord() throws StorageException {
        try {
            this.secret.clearKey();
        } catch (Exception e) {
            throw new StorageException("Can not clear key");
        }
        nameLen = 0;
        Util.arrayFillNonAtomic(name, (short) 0, (short) name.length, (byte) 0);
        Util.arrayFillNonAtomic(crc, (short) 0, (short) crc.length, (byte) 0);
        Util.arrayFillNonAtomic(tempArray, (short) 0, (short) tempArray.length, (byte) 0);
    }

    public byte isEmpty() {
        return (byte) (nameLen > 0 ? 1 : 0);
    }
}
