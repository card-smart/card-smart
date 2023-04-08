package applet;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.Cipher;

// Response codes from CardSmartApplet
import static applet.CardSmartApplet.*;


public class SecureChannel {

    /*
     * Secure channel constants
     */
    private static final short AES_BLOCK_SIZE = (short) 16;
    private static final short MAC_SIZE = (short) 16;
    private static final short PAIRING_SECRET_LENGTH = (short) 32;
    private static final short AES_KEY_LENGTH = (short) 32;

    /*
     * Algorithm implementations
     */
    RandomData random;
    private KeyAgreement ecdh;
    private Signature mac;
    MessageDigest sha512;
    Cipher aesCbc;

    private AESKey encryptionKey;
    private AESKey macKey;
    private KeyPair ecKeypair;
    private byte[] secret;
    private byte[] iv;
    private byte[] pairingSecret;

    private ECPublicKey ecpub;
    private ECPrivateKey ecpriv;

    /*
     * Initialize new secure channel instance, prepare algorithms and generate first card EC keypair
     */
    public SecureChannel() {}

    /**
     * Get the card's public key into the output buffer in plain form
     *
     * @param buffer the buffer
     * @param offset the offset in the buffer
     * @return the length of the public key
     */
    public short getCardPublicKey(byte[] buffer, short offset) {
        ECPublicKey pk = (ECPublicKey) ecKeypair.getPublic();
        return pk.getW(buffer, offset);
    }

    public void initSecureChannelObjects() throws StorageException, SecureChannelException, InvalidArgumentException, ConsistencyException {
        random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
        ecdh = KeyAgreement.getInstance(KeyAgreement.ALG_EC_SVDP_DH_PLAIN, false);
        mac = Signature.getInstance(Signature.ALG_AES_MAC_128_NOPAD, false);
        sha512 = MessageDigest.getInstance(MessageDigest.ALG_SHA_512, false);
        aesCbc = Cipher.getInstance(Cipher.ALG_AES_CBC_ISO9797_M2,false);

        // prepare objects for symmetric AES & MAC keys
        encryptionKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_AES_256, false);
        macKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_AES_256, false);

        // temporary buffer used for storing generated/derived secrets
        secret = JCSystem.makeTransientByteArray(PAIRING_SECRET_LENGTH, JCSystem.CLEAR_ON_DESELECT);

        // temporary buffer used for storing iv (= last MAC value seen from counterpart)
        iv = JCSystem.makeTransientByteArray(AES_BLOCK_SIZE, JCSystem.CLEAR_ON_DESELECT);

        // card EC keypair
        ecpub = (ECPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_EC_FP_PUBLIC, KeyBuilder.LENGTH_EC_FP_256, false);
        ecpriv = (ECPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_EC_FP_PRIVATE, KeyBuilder.LENGTH_EC_FP_256, false);

        this.ecKeypair = new KeyPair(ecpub, ecpriv);
        //this.ecKeypair = new KeyPair(KeyPair.ALG_EC_FP, KeyBuilder.LENGTH_EC_FP_256);

        try {
            ecKeypair.genKeyPair();
        } catch (CryptoException e) {
            if (e.getReason() == CryptoException.UNINITIALIZED_KEY) {
                throw new ConsistencyException();
            }
            if (e.getReason() == CryptoException.ILLEGAL_VALUE  || e.getReason() == CryptoException.ILLEGAL_USE || e.getReason() == CryptoException.INVALID_INIT || e.getReason() == CryptoException.NO_SUCH_ALGORITHM) {
                throw new InvalidArgumentException();
            }
            throw new StorageException();
        } catch (Exception e) {
            throw new SecureChannelException();
        }

        // persistent pairing secret used for secure channel after card initialization
        pairingSecret = new byte[PAIRING_SECRET_LENGTH];
    }

    /**
     * Initializes the SecureChannel instance with the pairing secret and new EC keypair.
     *
     * @param newPairingSecret the pairing secret
     * @param offset start of the pairing secret in the newPairingSecret buffer
     * @apiNote taken from status-keycard/SecureChannel.java
     */
    public void initSecureChannel(byte[] newPairingSecret, short offset) {
        // set new pairing secret after init command in the applet
        Util.arrayCopy(newPairingSecret, (short) 0, pairingSecret, offset, PAIRING_SECRET_LENGTH);
        // update keys
        ecKeypair.genKeyPair();
    }

    /**
     * Decrypts the content of the APDU by symmetric key
     * @param apduBuffer the APDU buffer [123 B] = key length [1 B] | EC public key [65 B] | IV [16 B] | encrypted(PIN | pairingSecret) [42 B]
     * @apiNote taken from status-keycard/SecureChannel.java
     */
    public void initDecrypt(byte[] apduBuffer) {
        // 1. initialize ECDH with card EC private key
        ecdh.init(ecKeypair.getPrivate());

        short publicKeyOffset = (short)(ISO7816.OFFSET_CDATA + 1);
        try {
            // 2. derive secret from card private key and incoming public key into secret buffer
            // Use public key from apdu buffer
            short publicKeyLength = apduBuffer[ISO7816.OFFSET_CDATA];
            ecdh.generateSecret(apduBuffer, publicKeyOffset, publicKeyLength, secret, (short) 0);
        } catch(Exception e) {
            ISOException.throwIt(RES_ERR_ECDH);
        }
        // 3. get IV from apduBuffer and prepare decryption
        short ivOffset = (short)(publicKeyOffset + apduBuffer[ISO7816.OFFSET_CDATA]);
        encryptionKey.setKey(secret, (short) 0);
        aesCbc.init(encryptionKey, Cipher.MODE_DECRYPT, apduBuffer, ivOffset, AES_BLOCK_SIZE);

        // 4. decrypt payload data part in place
        short payloadOffset = (short)(ivOffset + AES_BLOCK_SIZE);
        try {
            apduBuffer[ISO7816.OFFSET_LC] = (byte) aesCbc.doFinal(apduBuffer, payloadOffset,
                    (short) ((short) (apduBuffer[ISO7816.OFFSET_LC] & 0xff) - payloadOffset + ISO7816.OFFSET_CDATA),
                    apduBuffer, ISO7816.OFFSET_CDATA);
        } catch (Exception e) {
            ISOException.throwIt(RES_ERR_DECRYPT);
        }
    }

    /**
     * Open secure channel and generate AES keys for encryption and MAC
     * @param apdu CLA | INS | P1 | P2 | Lc | public key
     * @apiNote taken from status-keycard/SecureChannel.java
     */
    public void openSecureChannel(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        // 1. initialize ECDH with card's private key
        ecdh.init(ecKeypair.getPrivate());
        short len;

        try {
            // 2. get derived secret from card's private key and tool's public key
            len = ecdh.generateSecret(apduBuffer, ISO7816.OFFSET_CDATA, apduBuffer[ISO7816.OFFSET_LC], secret, (short) 0);
        } catch(Exception e) {
            ISOException.throwIt(RES_ERR_SECURE_CHANNEL);
            return;
        }
        // 3. generate random salt [32 B] and IV [16 B] for tool
        random.generateData(apduBuffer, (short) 0, (short) (PAIRING_SECRET_LENGTH + AES_BLOCK_SIZE));
        // 4. generate hash from derived secret, pairing secret and salt
        sha512.update(secret, (short) 0, len);
        sha512.update(pairingSecret, (short) 0, PAIRING_SECRET_LENGTH);
        sha512.doFinal(apduBuffer, (short) 0, PAIRING_SECRET_LENGTH, secret, (short) 0);
        // 5. set symmetric keys and reset secret buffer
        encryptionKey.setKey(secret, (short) 0);
        macKey.setKey(secret, AES_KEY_LENGTH);
        Util.arrayFillNonAtomic(secret, (short) 0, PAIRING_SECRET_LENGTH, (byte) 0);
        // 6. set generated IV as IV for next decryption
        Util.arrayCopyNonAtomic(apduBuffer, PAIRING_SECRET_LENGTH, iv, (short) 0, AES_BLOCK_SIZE);
        // 7. send salt and IV to tool
        apdu.setOutgoingAndSend((short) 0, (short) (PAIRING_SECRET_LENGTH + AES_BLOCK_SIZE));
    }

    /**
     * Verify MAC tag which is part of the APDU buffer
     *
     * @param apduBuffer CLA | INS | P1 | P2 | Lc | encrypted data | MAC tag [16 B]
     * @param dataLength length of encrypted data
     * @param macOffset  position in buffer where tag starts
     */
    private boolean verifyMAC(byte[] apduBuffer, short dataLength, short macOffset) {
        // 1. initialize MAC algorithm with AES MAC secret key
        mac.init(macKey, Signature.MODE_VERIFY);
        // 2. load data (including instruction bytes) and verify the tag
        return mac.verify(apduBuffer, (short) 0, (short) (5 + dataLength), apduBuffer, macOffset, MAC_SIZE);
    }

    /**
     * Decrypt APDU data buffer in place
     * @param apduBuffer CLA | INS | P1 | P2 | Lc | encrypted payload [max 240 B] | MAC tag [16 B]
     * @apiNote taken from status-keycard/SecureChannel.java
     */
    public void decryptAPDU(byte[] apduBuffer) {
        // 1. get length (Lc) of the APDU (it should be divisible by AES_BLOCK_SIZE)
        short Lc = apduBuffer[ISO7816.OFFSET_LC];
        // 2. get length of the APDU data without MAC tag (it should be divisible by AES_BLOCK_SIZE)
        short payloadLength = (short) (Lc - MAC_SIZE);
        if (payloadLength % AES_BLOCK_SIZE !=  0) {
            ISOException.throwIt(RES_ERR_DATA_LENGTH);
        }
        // 3. verify MAC tag
        short macOffset = (short) (ISO7816.OFFSET_CDATA + (Lc - MAC_SIZE));
        if (!verifyMAC(apduBuffer, payloadLength, macOffset)) {
            closeSecureChannel();
            ISOException.throwIt(RES_ERR_MAC);
        }
        try {
            // 4. initialize AES cipher with IV (last MAC generated by card in response)
            aesCbc.init(encryptionKey, Cipher.MODE_DECRYPT, iv, (short) 0, AES_BLOCK_SIZE);
            // 5. store MAC tag as IV for next encryption of response
            Util.arrayCopyNonAtomic(apduBuffer, macOffset, iv, (short) 0, MAC_SIZE);
            // 6. decrypt the APDU buffer
            short decryptedLength = aesCbc.doFinal(apduBuffer, ISO7816.OFFSET_CDATA, payloadLength, apduBuffer, ISO7816.OFFSET_CDATA);
            // 7. set decrypted length to the Lc byte
            apduBuffer[ISO7816.OFFSET_LC] = (byte) decryptedLength;
        } catch (Exception e) {
            ISOException.throwIt(RES_ERR_DECRYPTION);
        }
    }

    /**
     * Compute and append MAC tag for response
     * @param responseBuffer ... | encrypted(data | SW1 | SW2)
     * @param responseLength length of encrypted data which should be MACed
     * @param responseOffset offset where the response data starts
     * @apiNote result: ... | encrypted(data | SW1 | SW2) | MAC tag
     */
    private void computeMAC(byte[] responseBuffer, short responseLength, short responseOffset) {
        // 1. response data should be already encrypted by AES, check the correct length
        if (responseLength % AES_BLOCK_SIZE != 0) {
            ISOException.throwIt(RES_ERR_MAC);
        }
        // 2. set MAC key for creating a tag
        mac.init(macKey, Signature.MODE_SIGN);
        // 3. load data & store tag just after the encrypted data
        mac.sign(responseBuffer, responseOffset, responseLength, responseBuffer, (short) (responseOffset + responseLength));
    }

    /**
     * Encrypt response APDU in place and append MAC tag after the data
     * @param responseBuffer ... | data | SW1 | SW2
     * @param responseLength length of data + 2 (for SW1 and SW2)
     * @param responseOffset where the data starts withing responseBuffer
     * @return length of payload (encrypted data + MAC)
     * @apiNote result: ... | encrypted(data | SW1 | SW2) | MAC tag
     * @apiNote taken from status-keycard/SecureChannel.java
     */
    public short encryptResponse(byte[] responseBuffer, short responseLength, short responseOffset, short SW) {
        // 1. Add SW after response into the buffer
        Util.setShort(responseBuffer, (short) (responseOffset + responseLength), SW);
        responseLength += 2;
        short encryptedLength = 0;
        try {
            // 2. prepare AES
            aesCbc.init(encryptionKey, Cipher.MODE_ENCRYPT, secret, (short) 0, AES_BLOCK_SIZE);
            // 3. encrypt the resBuffer in place
            encryptedLength = aesCbc.doFinal(responseBuffer, responseOffset, responseLength, responseBuffer, responseOffset);
        } catch (Exception e) {
            ISOException.throwIt(RES_ERR_ENCRYPTION);
        }
        // 4. compute MAC of the whole encrypted part
        computeMAC(responseBuffer, encryptedLength, responseOffset);
        // 5. store MAC tag as IV for next decryption of APDU
        short macOffset = (short) (responseOffset + responseLength);
        Util.arrayCopyNonAtomic(responseBuffer, macOffset, iv, (short) 0, AES_BLOCK_SIZE);
        return (short) (encryptedLength + MAC_SIZE);
    }

    public void closeSecureChannel() {
        encryptionKey.clearKey();
        macKey.clearKey();
        Util.arrayFillNonAtomic(secret, (short) 0, PAIRING_SECRET_LENGTH, (byte) 0);
        Util.arrayFillNonAtomic(iv, (short) 0, AES_BLOCK_SIZE, (byte) 0);
    }
}
