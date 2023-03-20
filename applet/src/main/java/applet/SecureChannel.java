package applet;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.Cipher;

public class SecureChannel {

    /*
     * Secure channel constants
     */
    private static final short AES_BLOCK_SIZE = (short) 16;
    private static final short PAIRING_SECRET_LENGTH = (short) 32;
    private static final short AES_KEY_LENGTH = (short) 32;
    private static final short NONCE_LENGTH = (short) 32;

    /*
     * Algorithm implementations
     */
    RandomData random;
    private final KeyAgreement ecdh;
    private Signature mac;
    MessageDigest sha512;
    Cipher aesCbc;

    private final AESKey encryptionKey;
    private final AESKey macKey;
    private KeyPair ecKeypair;
    private final byte[] secret;
    private final byte[] pairingSecret;

    /*
     * Initialize new secure channel instance, prepare algorithms and generate static card EC keypair
     */
    public SecureChannel() {
        random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
        ecdh = KeyAgreement.getInstance(KeyAgreement.ALG_EC_SVDP_DH_PLAIN, false);
        mac = Signature.getInstance(Signature.ALG_AES_MAC_128_NOPAD, false);
        sha512 = MessageDigest.getInstance(MessageDigest.ALG_SHA_512, false);
        aesCbc = Cipher.getInstance(Cipher.ALG_AES_CBC_ISO9797_M2,false);

        encryptionKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_AES_256, false);
        macKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_AES_256, false);

        secret = JCSystem.makeTransientByteArray(NONCE_LENGTH, JCSystem.CLEAR_ON_DESELECT);

        KeyPair ecKeypair = new KeyPair(KeyPair.ALG_EC_FP, KeyBuilder.LENGTH_EC_FP_256);
        ecKeypair.genKeyPair();

        pairingSecret = new byte[PAIRING_SECRET_LENGTH];
    }

    /*
     * Get card's public key
     */
    public ECPublicKey getCardPublicKey(){
        return (ECPublicKey)ecKeypair.getPublic();
    }

    /**
     * Initializes the SecureChannel instance with the pairing secret.
     *
     * @param newPairingSecret the pairing secret
     * @apiNote taken from status-keycard/SecureChannel.java
     */
    public void initSecureChannel(byte[] newPairingSecret, short offset) {
        // set new pairing secret after init command in the applet
        Util.arrayCopy(newPairingSecret, offset, pairingSecret, (short) 0, PAIRING_SECRET_LENGTH);
        // update keys
        ecKeypair.genKeyPair();
    }

    /**
     * Decrypts the content of the APDU by generating an AES key using EC-DH. Usable only with specific commands.
     * @param apduBuffer the APDU buffer
     * @apiNote taken from status-keycard/SecureChannel.java
     */
    public void initDecrypt(byte[] apduBuffer) {
        // apdu payload = EC public key | IV [16 B] | encrypted(PIN | pairingSecret)
        ecdh.init(ecKeypair.getPrivate());

        short publicKeyOffset = (short)(ISO7816.OFFSET_CDATA + 1);
        try {
            // Use public key from apdu buffer to derive secret used for encryption of payload
            ecdh.generateSecret(apduBuffer, publicKeyOffset, apduBuffer[ISO7816.OFFSET_CDATA], secret, (short) 0);
        } catch(Exception e) {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            return;
        }
        // get IV from apduBuffer and prepare decryption
        short ivOffset = (short)(publicKeyOffset + apduBuffer[ISO7816.OFFSET_CDATA]);
        encryptionKey.setKey(secret, (short) 0);
        aesCbc.init(encryptionKey, Cipher.MODE_DECRYPT, apduBuffer, ivOffset, AES_BLOCK_SIZE);

        short payloadOffset = (short)(ivOffset + AES_BLOCK_SIZE);
        apduBuffer[ISO7816.OFFSET_LC] = (byte) aesCbc.doFinal(apduBuffer, payloadOffset,
                (short)((short)(apduBuffer[ISO7816.OFFSET_LC] & 0xff) - payloadOffset + ISO7816.OFFSET_CDATA),
                apduBuffer, ISO7816.OFFSET_CDATA);
    }

    /**
     * Open secure channel and generate AES card' keys
     * @param apdu DATA: tool's public key, Lc: length of public key
     * @apiNote taken from status-keycard/SecureChannel.java
     */
    public void openSecureChannel(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();

        ecdh.init(ecKeypair.getPrivate());
        short len;

        try {
            // 1. get derived secret from card private key and tool public key
            len = ecdh.generateSecret(apduBuffer, ISO7816.OFFSET_CDATA, apduBuffer[ISO7816.OFFSET_LC], secret, (short) 0);
        } catch(Exception e) {
            // Throw better APDU
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            return;
        }
        // 2. generate random salt for tool
        // TODO: IV?
        random.generateData(apduBuffer, (short) 0, PAIRING_SECRET_LENGTH);
        // 3. generate hash from salt, pairing secret and derived secret
        sha512.update(secret, (short) 0, len);
        sha512.update(pairingSecret, (short) 0, PAIRING_SECRET_LENGTH);
        sha512.doFinal(apduBuffer, (short) 0, PAIRING_SECRET_LENGTH, secret, (short) 0);
        // 4. set symmetric keys
        encryptionKey.setKey(secret, (short) 0);
        macKey.setKey(secret, AES_KEY_LENGTH);
        // TODO: set second part of secret to be IV
        //Util.arrayCopyNonAtomic(apduBuffer, PAIRING_SECRET_LENGTH, secret, (short) 0, AES_BLOCK_SIZE);
        //Util.arrayFillNonAtomic(secret, SC_BLOCK_SIZE, (short) (secret.length - SC_BLOCK_SIZE), (byte) 0);
        // 5. send salt to tool
        apdu.setOutgoingAndSend((short) 0, PAIRING_SECRET_LENGTH);
    }
}
