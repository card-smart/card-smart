package applet;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.Checksum;
import javacard.security.HMACKey;
import javacard.security.KeyBuilder;

import static applet.CardSmartApplet.*;

public class Record {
    private final byte[] name;
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

        /* Prepare empty secret */
        byte[] initSecret = JCSystem.makeTransientByteArray((short) 64, JCSystem.CLEAR_ON_DESELECT);
        secret = (HMACKey) KeyBuilder.buildKey(KeyBuilder.TYPE_HMAC, KeyBuilder.LENGTH_HMAC_SHA_256_BLOCK_64, false);
        secret.setKey(initSecret, (short) 0, SECRET_MAX_LEN);

        /* Initialize checksum */
        checksum = Checksum.getInstance(Checksum.ALG_ISO3309_CRC32, false);
        crc = new byte[CRC_LEN];
        tempArray = JCSystem.makeTransientByteArray((short) CRC_LEN, JCSystem.CLEAR_ON_DESELECT);
    }

    /*
     * Set new name to record
     */
    private void setName(byte[] name, byte length) throws InvalidArgumentException {
        if (length < NAME_MIN_LEN || length > NAME_MAX_LEN) {
            throw new InvalidArgumentException("Wrong length of the name.");
        }
        Util.arrayCopyNonAtomic(name, (short) 0, this.name, (byte) 0, (byte) length);
    }

    /*
     * Get name of the record
     */
    public byte[] getName() {
        return name;
    }

    /*
     * Clear previous secret and set its name
     */
    private void setSecret(byte[] secret, byte length) throws InvalidArgumentException {
        if (length < SECRET_MIN_LEN || length > SECRET_MAX_LEN) {
            throw new InvalidArgumentException("Wrong length of the secret.");
        }
        this.secret.clearKey();
        this.secret.setKey(secret, (byte) 0, length);
    }

    private byte checkCRC(byte[] crc, byte[] current) {
        for (int i = 0; i < CRC_LEN; i++) {
            if (crc[i] != current[i]) {
                return 1;
            }
        }
        return 0;
    }

    /*
     * Get value of secret
     */
    public byte getSecret(byte[] buffer, byte length) throws InvalidArgumentException, ConsistencyException {
        if (length < SECRET_MIN_LEN || length > SECRET_MAX_LEN) {
            throw new InvalidArgumentException("Wrong length of the buffer for secret.");
        }

        /* Get value of secret */
        byte secretLen = this.secret.getKey(buffer, length);

        /* Compute checksum of secret */
        checksum.doFinal(buffer, (short) 0, secretLen, tempArray, (short) 0);
        if (checkCRC(crc, tempArray) != 0) {
            throw new ConsistencyException("CRC does not match");
        }

        return secretLen;
    }

    /*
     * Initialize record value and checksum
     */
    public void initRecord(byte[] name, byte nameLen, byte[] secret, byte secretLen) throws InvalidArgumentException {
        this.setName(name, nameLen);
        this.setSecret(secret, secretLen);
        checksum.doFinal(secret, (short) 0, secretLen, crc, (short) 0);
    }
}
