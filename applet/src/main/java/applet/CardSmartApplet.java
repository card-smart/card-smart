package applet;

import javacard.framework.*;

public class CardSmartApplet extends Applet {

    /*
     * APDU Instruction Codes
     */
    /* CLA byte, specific for the applet */
    protected static final byte CLA_CARDSMARTAPPLET = (byte)0xB0;
    /* Unsecure Get Card EC Public Key */
    protected static final byte INS_GET_PUBLIC_KEY = (byte)0x40;
    /* Set applet into Initialized mode */
    protected static final byte INS_INIT = (byte)0x41;
    /* Unsecure Open Secure Channel */
    protected static final byte INS_OPEN_SC = (byte)0x41;
    /* Unsecure Close Secure Channel */
    protected static final byte INS_CLOSE_SC = (byte)0x42;
    /* Unsecure applet operations */
    protected static final byte INS_GET_NAMES = (byte)0x50;
    protected static final byte INS_PIN_TRIES = (byte)0x60;
    protected static final byte INS_PIN_VERIFY = (byte)0x61;
    protected static final byte INS_PIN_CHANGE = (byte)0x62;
    protected static final byte INS_GET_SECRET = (byte)0x71;
    protected static final byte INS_STORE_SECRET = (byte)0x80;
    protected static final byte INS_DELETE_SECRET = (byte)0x81;
    /* Secure applet operations */
    protected static final byte S_INS_GET_NAMES = (byte)0x30;
    protected static final byte S_INS_PIN_TRIES = (byte)0x31;
    protected static final byte S_INS_PIN_VERIFY = (byte)0x32;
    protected static final byte S_INS_PIN_CHANGE = (byte)0x33;
    protected static final byte S_INS_GET_SECRET = (byte)0x34;
    protected static final byte S_INS_STORE_SECRET = (byte)0x35;
    protected static final byte S_INS_DELETE_SECRET = (byte)0x36;

    /*
     * Response Codes
     */
    protected static final short RES_SUCCESS = (short)0x9000;
    /* Secure channel */
    protected static final short RES_ERR_SECURE_CHANNEL = (short)0x6A00;
    protected static final short RES_ERR_DECRYPTION = (short)0x6A01;
    protected static final short RES_ERR_MAC = (short)0x6A02;
    protected static final short RES_ERR_UNINITIALIZED = (short)0x6A02;
    protected static final short RES_ERR_ECDH = (short)0x6A03;
    protected static final short RES_ERR_DECRYPT = (short)0x6A04;
    /* Operations */
    protected static final short RES_ERR_GENERAL = (short)0x6B00;
    protected static final short RES_ERR_NOT_LOGGED = (short)0x6B01;
    protected static final short RES_ERR_RESET = (short)0x6B02;
    protected static final short RES_ERR_PIN_POLICY = (short)0x6B03;
    protected static final short RES_ERR_STORAGE_FULL = (short)0x6B04;
    protected static final short RES_ERR_NAME_POLICY = (short)0x6B05;
    protected static final short RES_ERR_SECRET_POLICY = (short)0x6B06;
    protected static final short RES_ERR_NO_DATA = (short)0x6B07;
    protected static final short RES_ERR_INPUT_DATA = (short)0x6B08;
    /* Unsupported instructions */
    protected static final short RES_UNSUPPORTED_CLA = (short)0x6C00;
    protected static final short RES_UNSUPPORTED_INS = (short)0x6C01;

    /*
     * Constants & Policy
     */
    /* PIN constants */
    private static final byte[] DEFAULT_PIN = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
    protected static final byte PIN_MIN_LEN = (byte)4;
    protected static final byte PIN_MAX_LEN = (byte)10;
    protected static final byte PIN_MAX_TRIES = (byte)5;
    /* Name constants */
    protected static final byte NAME_MIN_LEN = (byte)4;
    protected static final byte NAME_MAX_LEN = (byte)10;
    /* Secret' constants */
    protected static final byte SECRET_MIN_LEN = (byte)2;
    protected static final byte SECRET_MAX_LEN = (byte)32;

    /* Cryptographic instances */
    private OwnerPIN pin = null;

    /*
     * Other instances
     */

    FileSystem fileSystem = null;
    SecureChannel secureChannel = null;

    private static final short TEMP_ARRAY_LEN = (short) 256;
    //private byte[] tempArray = null;
    private boolean[] isUserAuthenticated = null;
    private boolean[] isAppletInitialized = null;

    public CardSmartApplet(byte[] bArray, short bOffset, byte bLength) {

        /* Create temporary array */
        //this.tempArray = JCSystem.makeTransientByteArray(TEMP_ARRAY_LEN, JCSystem.CLEAR_ON_DESELECT);

        /* Set initial PIN */
        this.pin = new OwnerPIN(PIN_MAX_TRIES, PIN_MAX_LEN); // 5 tries, max 10 digits in pin
        this.pin.update(DEFAULT_PIN, (short) 0, PIN_MAX_LEN);

        /* Create array for user authentication */
        this.isUserAuthenticated = JCSystem.makeTransientBooleanArray((short) 1, JCSystem.CLEAR_ON_DESELECT);

        /* Create array for denoting the applet is initialized */
        this.isAppletInitialized = new boolean[1];

        /* Initialize filesystem with empty records */
        this.fileSystem = new FileSystem();

        /* Create instance of SecureChannel class */
        secureChannel = new SecureChannel();

        register();
    }

    public static void install(byte[] bArray, short bOffset, byte bLength)
    {
        new CardSmartApplet(bArray, bOffset, bLength);
    }

    @Override
    public boolean select() {
        // TODO: Clear session data here
        return true;
    }

    @Override
    public void process(APDU apdu) throws ISOException {
        // get the buffer with incoming APDU
        byte[] apduBuffer = apdu.getBuffer();

        // ignore the applet select command dispached to the process
        if (selectingApplet()) {
            return;
        }

        try {
            // APDU instruction parser
            if (apduBuffer[ISO7816.OFFSET_CLA] == CLA_CARDSMARTAPPLET) {
                switch (apduBuffer[ISO7816.OFFSET_INS]) {
                    case INS_GET_PUBLIC_KEY:
                        this.getPublicKey(apdu);
                        break;
                    case INS_INIT:
                        this.init(apdu);
                        break;
                    case INS_PIN_TRIES:
                        this.getPINTries(apdu);
                        break;
                    case INS_PIN_VERIFY:
                        this.verifyPIN(apdu);
                        break;
                    case INS_PIN_CHANGE:
                        this.changePIN(apdu);
                        break;
                    case INS_STORE_SECRET:
                        this.storeSecret(apdu);
                        break;
                    case INS_GET_NAMES:
                        this.getNames(apdu);
                        break;
                    default:
                        // The INS code is not supported by the dispatcher
                        ISOException.throwIt(RES_UNSUPPORTED_INS);
                        break;
                }
            } else {
                ISOException.throwIt(RES_UNSUPPORTED_CLA);
            }
        // TODO: Capture all reasonable exceptions and change into readable ones (instead of 0x6f00)
        } catch (ISOException e) {
            throw e; // Our exception from code, just re-emit
        } catch (Exception e) {
            ISOException.throwIt(RES_ERR_GENERAL);
        }
    }

    /**
     * Returns true when applet is initialized
     */
    private boolean getAppletInitialized() {
        return this.isAppletInitialized[0];
    }

    /**
     * Get public key of card
     *
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = none, DATA = none
     * @RESPONSE   ECC public key
     * @apiNote works in both uninitialized and initialized state
     */
    void getPublicKey(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();

        // some key should be always generated
        short keyLength = secureChannel.getCardPublicKey(apduBuffer, ISO7816.OFFSET_CDATA);
        apdu.setOutgoingAndSend((short) 0, keyLength);
    }

    /**
     * Initialize applet
     *
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = max 0x7B, DATA = EC public key (LV encoded) + IV + encrypted payload
     * @RESPONSE   none
     * @apiNote authentication not required
     * @apiNote works in both uninitialized and initialized state
     */
    void init(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();

        // 1. if applet is initialized already and PIN was verified, first delete all data and reset PIN
        if (this.getAppletInitialized()) {
            if (this.getUserAuthenticated()) {
                try {
                    this.resetToDefault();
                } catch (StorageException e) {
                    ISOException.throwIt(RES_ERR_GENERAL);
                }
            } else {
                ISOException.throwIt(RES_ERR_GENERAL);
            }
        }

        // 2. decrypt apduBuffer to obtain PIN and pairingSecret for secure channel
        secureChannel.initDecrypt(apduBuffer);

        // 3. get PIN of decrypted apduBuffer
        // TODO: Check PIN policy
        pin.update(apduBuffer, ISO7816.OFFSET_CDATA, PIN_MAX_LEN);

        // 4. set pairingSecret and update current card EC keypair
        secureChannel.initSecureChannel(apduBuffer, (short) (ISO7816.OFFSET_CDATA + PIN_MAX_LEN));
    }

    /**
     * Set applet to authenticated state
     * @param isAuthenticated value to be set
     */
    private void setUserAuthenticated(boolean isAuthenticated) {
        this.isUserAuthenticated[0] = isAuthenticated;
    }

    /**
     * Get authentication state of applet
     */

    private boolean getUserAuthenticated() {
        return this.isUserAuthenticated[0];
    }

    /**
     * Get remaining PIN tries
     *
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = max 0x7B, DATA = EC public key (LV encoded) + IV + encrypted payload
     * @RESPONSE   2 B (short) of remaining PIN tries
     * @apiNote authentication not required
     * @apiNote works in both uninitialized and initialized state
     */
    void getPINTries(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        //short dataLength = apdu.setIncomingAndReceive();

        byte tries = pin.getTriesRemaining();
        this.fileSystem.tempArray[0] = tries;
        Util.arrayCopyNonAtomic(this.fileSystem.tempArray, (short) 0, apduBuffer, ISO7816.OFFSET_CDATA, (short) 1);

        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, (short) 1);
    }

    /**
     * Verify given PIN and set card into authenticated state
     *
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = max 0x7B, DATA = EC public key (LV encoded) + IV + encrypted payload
     * @RESPONSE   none
     * @apiNote authentication not required
     * @apiNote works in both uninitialized and initialized state
     */
    void verifyPIN(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();

        /* Verify pin*/
        if(!this.pin.check(apduBuffer, ISO7816.OFFSET_CDATA, PIN_MAX_LEN)) {
            byte tries = this.pin.getTriesRemaining();
            if (tries == 0) {
                try {
                    this.resetToDefault();
                } catch (StorageException e) {
                    ISOException.throwIt(RES_ERR_GENERAL);
                }
            }
            this.setUserAuthenticated(false);
            ISOException.throwIt(RES_ERR_NOT_LOGGED);
        }
        this.setUserAuthenticated(true);
    }

    /**
     * Change current PIN to the PIN in data
     *
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = max 0x0A, DATA = EC public key (LV encoded) + IV + encrypted payload
     * @RESPONSE   none
     * @apiNote authentication required
     * @apiNote works in both uninitialized and initialized state
     */
    void changePIN(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();

        /* Check whether user is authenticated */
        if (!this.getUserAuthenticated()) {
            ISOException.throwIt(RES_ERR_NOT_LOGGED);
        }
        /* Check that PIN has correct length = maximal length */
        if (dataLength != PIN_MAX_LEN) {
            ISOException.throwIt(RES_ERR_PIN_POLICY);
        }
        /* Set new user PIN */
        pin.update(apduBuffer, ISO7816.OFFSET_CDATA, (byte) dataLength);
    }

    /**
     * Store secret into card filesystem with given access name
     *
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c, DATA = name length [1 B] | name [max 10 B] | secret length [1 B] | secret [max 64 B]
     * @RESPONSE   none
     * @apiNote authentication required
     * @apiNote works in both uninitialized and initialized state
     */
    void storeSecret(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();

        /* Check whether user is authenticated */
        if (!this.getUserAuthenticated()) {
            ISOException.throwIt(RES_ERR_NOT_LOGGED);
        }
        /* Create record in filesystem */
        try {
            byte nameLength = apduBuffer[ISO7816.OFFSET_CDATA];
            short nameOffset = 1 + ISO7816.OFFSET_CDATA;
            byte secretLength = apduBuffer[ISO7816.OFFSET_CDATA + 1 + nameLength];
            short secretOffset = (short) (1 + ISO7816.OFFSET_CDATA + 1 + nameLength);
            fileSystem.createRecord(apduBuffer, nameLength, nameOffset, secretLength, secretOffset);
        } catch (StorageException e) {
            ISOException.throwIt(RES_ERR_STORAGE_FULL);
        } catch (InvalidArgumentException e) {
            ISOException.throwIt(RES_ERR_SECRET_POLICY);
        }
    }

    /**
     * Get names of all secrets stored in the card
     *
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = none, DATA = none
     * @RESPONSE   LV values of names concatenated together
     * @apiNote authentication not required
     * @apiNote works in both uninitialized and initialized state
     */
    void getNames(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();

        /* User does not have to be authenticated */
        short namesLength = 0;
        try {
            /* Get all names into the temporary buffer */
            namesLength = fileSystem.getAllNames(this.fileSystem.tempArray);
            /* Copy result into response buffer*/
            Util.arrayCopyNonAtomic(fileSystem.tempArray, (short) 0, apduBuffer, ISO7816.OFFSET_CDATA,namesLength);
        } catch (StorageException e) {
            ISOException.throwIt(RES_ERR_STORAGE_FULL);
        } catch (InvalidArgumentException e) {
            ISOException.throwIt(RES_ERR_SECRET_POLICY);
        }
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, namesLength);
    }

    /**
     * Reset data on card to default state
     * It does not influence the initialization state itself
     */
    private void resetToDefault() throws StorageException {
        //Util.arrayFillNonAtomic(tempArray, (short) 0, (short) TEMP_ARRAY_LEN, (byte) 0);
        eraseSecretData();
        this.pin.reset();
        this.pin.update(DEFAULT_PIN, (short) 0, PIN_MAX_LEN);
    }

    /**
     * Erase data from filesystem
     */
    private void eraseSecretData() throws StorageException {
        fileSystem.eraseData();
    }
}
