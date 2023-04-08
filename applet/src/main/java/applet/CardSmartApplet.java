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
    protected static final byte INS_OPEN_SC = (byte)0x42;
    /* Unsecure Close Secure Channel */
    protected static final byte INS_CLOSE_SC = (byte)0x44;
    /* Unsecure applet operations */
    protected static final byte INS_GET_NAMES = (byte)0x20;
    protected static final byte INS_PIN_TRIES = (byte)0x21;
    protected static final byte INS_PIN_VERIFY = (byte)0x22;
    protected static final byte INS_PIN_CHANGE = (byte)0x23;
    protected static final byte INS_GET_SECRET = (byte)0x24;
    protected static final byte INS_STORE_SECRET = (byte)0x25;
    protected static final byte INS_DELETE_SECRET = (byte)0x26;
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
    protected static final short RES_ERR_ECDH = (short)0x6A03;
    protected static final short RES_ERR_UNINITIALIZED = (short)0x6A04;
    protected static final short RES_ERR_INITIALIZED = (short)0x6A05;
    protected static final short RES_ERR_ENCRYPTION = (short)0x6A06;
    protected static final short RES_ERR_DATA_LENGTH = (short)0x6A07;

    /* Operations */
    protected static final short RES_ERR_GENERAL = (short)0x6B00;
    protected static final short RES_ERR_NOT_LOGGED = (short)0x6B01;
    protected static final short RES_ERR_RESET = (short)0x6B02;
    protected static final short RES_ERR_PIN_POLICY = (short)0x6B03;
    protected static final short RES_ERR_STORAGE = (short)0x6B04;
    protected static final short RES_ERR_NAME_POLICY = (short)0x6B05;
    protected static final short RES_ERR_SECRET_POLICY = (short)0x6B06;
    // protected static final short RES_ERR_NO_DATA = (short)0x6B07;
    // protected static final short RES_ERR_INPUT_DATA = (short)0x6B08;
    /* Unsupported instructions */
    protected static final short RES_UNSUPPORTED_CLA = (short)0x6C00;
    protected static final short RES_UNSUPPORTED_INS = (short)0x6C01;

    /*
     * Constants & Policy
     */
    /* PIN constants */
    private static final byte[] DEFAULT_PIN = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
    protected static final byte PIN_MAX_LEN = (byte)10;
    protected static final byte PIN_MAX_TRIES = (byte)5;

    /* Cryptographic instances */
    private final OwnerPIN pin;

    /*
     * Other instances
     */

    FileSystem fileSystem;
    SecureChannel secureChannel;
    private final boolean[] isUserAuthenticated;
    private final boolean[] isAppletInitialized;

    public CardSmartApplet(byte[] bArray, short bOffset, byte bLength) {
        /* Set initial PIN */
        this.pin = new OwnerPIN(PIN_MAX_TRIES, PIN_MAX_LEN); // 5 tries, 10 digits in pin
        this.pin.update(DEFAULT_PIN, (short) 0, PIN_MAX_LEN);

        /* Create array for user authentication */
        this.isUserAuthenticated = JCSystem.makeTransientBooleanArray((short) 1, JCSystem.CLEAR_ON_DESELECT);
        this.setUserAuthenticated(false);

        /* Create array for denoting the applet is initialized */
        this.isAppletInitialized = new boolean[1];
        this.setAppletInitialized(false);

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
        // log out user by applet selection
        this.setUserAuthenticated(false);
        return true;
    }

    @Override
    public void process(APDU apdu) throws ISOException {
        // get the buffer with incoming APDU
        byte[] apduBuffer = apdu.getBuffer();

        // ignore the applet select command dispatched to the process
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
                    case INS_OPEN_SC:
                        this.openSecureChannel(apdu);
                        break;
                    case INS_CLOSE_SC:
                        this.closeSecureChannel();
                        break;
                    case INS_PIN_TRIES:
                        this.unsecureGetPINTries(apdu);
                        break;
                    case S_INS_PIN_TRIES:
                        this.secureGetPINTries(apdu);
                        break;
                    case INS_PIN_VERIFY:
                        this.unsecureVerifyPIN(apdu);
                        break;
                    case S_INS_PIN_VERIFY:
                        this.secureVerifyPIN(apdu);
                        break;
                    case INS_PIN_CHANGE:
                        this.unsecureChangePIN(apdu);
                        break;
                    case S_INS_PIN_CHANGE:
                        this.secureChangePIN(apdu);
                        break;
                    case INS_STORE_SECRET:
                        this.unsecureStoreSecret(apdu);
                        break;
                    case S_INS_STORE_SECRET:
                        this.secureStoreSecret(apdu);
                        break;
                    case INS_GET_NAMES:
                        this.unsecureGetNames(apdu);
                        break;
                    case S_INS_GET_NAMES:
                        this.secureGetNames(apdu);
                        break;
                    case INS_GET_SECRET:
                        this.unsecureGetSecret(apdu);
                        break;
                    case S_INS_GET_SECRET:
                        this.secureGetSecret(apdu);
                        break;
                    case INS_DELETE_SECRET:
                        this.unsecureDeleteSecret(apdu);
                        break;
                    case S_INS_DELETE_SECRET:
                        this.secureDeleteSecret(apdu);
                        break;
                    default:
                        // The INS code is not supported by the dispatcher
                        ISOException.throwIt(RES_UNSUPPORTED_INS);
                        break;
                }
            } else {
                ISOException.throwIt(RES_UNSUPPORTED_CLA);
            }
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
     * Returns true when applet is initialized
     * @param isInitialized value to be set
     */
    private void setAppletInitialized(boolean isInitialized) {
        this.isAppletInitialized[0] = isInitialized;
    }

    /**
     * Get public key of card
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = none, DATA = none
     * @RESPONSE   ECC public key
     * @apiNote works in both uninitialized and initialized state
     */
    void getPublicKey(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        // some key should be always generated
        short keyLength = secureChannel.getCardPublicKey(apduBuffer, ISO7816.OFFSET_CDATA);
        apdu.setOutgoingAndSend((short) 0, keyLength);
    }

    /**
     * Initialize applet
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
                    ISOException.throwIt(RES_ERR_STORAGE);
                }
            } else {
                ISOException.throwIt(RES_ERR_NOT_LOGGED);
            }
        }

        // 2. decrypt apduBuffer to obtain PIN and pairingSecret for secure channel
        secureChannel.initDecrypt(apduBuffer);

        // 3. get PIN of decrypted apduBuffer
        // TODO: Check PIN policy
        pin.update(apduBuffer, ISO7816.OFFSET_CDATA, PIN_MAX_LEN);

        // 4. set pairingSecret and update current card EC keypair
        secureChannel.initSecureChannel(apduBuffer, (short) (ISO7816.OFFSET_CDATA + PIN_MAX_LEN));

        // 5. set applet into initialized state
        this.setAppletInitialized(true);
    }

    /**
     * Open secure channel for current communication
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = length of the tool's public key, DATA = EC public key
     * @RESPONSE   salt [32 B] | IV [16] B
     * @apiNote authentication not required
     * @apiNote works in initialized state
     */
    void openSecureChannel(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();

        // 1. if applet is not initialized, throw error
        if (!this.getAppletInitialized()) {
            ISOException.throwIt(RES_ERR_UNINITIALIZED);
        }

        // 2. open secure channel
        secureChannel.openSecureChannel(apdu);
    }

    /**
     * Close secure channel for current communication
     * @APDU       P1 = 0, P2 = 0, L_c = none, DATA = none
     * @RESPONSE   salt [32 B] | IV [16] B
     * @apiNote authentication not required
     * @apiNote works in initialized state
     */
    void closeSecureChannel() {
        // 1. if applet is not initialized, throw error
        if (!this.getAppletInitialized()) {
            ISOException.throwIt(RES_ERR_UNINITIALIZED);
        }

        // 2. close secure channel
        secureChannel.closeSecureChannel();
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
     * Copy remaining PIN tries into response buffer
     * @param responseBuffer buffer for response
     * @apiNote works in both uninitialized and initialized state
     */
    private void getPINTries(byte[] responseBuffer) {
        byte tries = pin.getTriesRemaining();
        responseBuffer[ISO7816.OFFSET_CDATA] = tries;
    }

    /**
     * Unsecure get and send remaining PIN tries
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0
     * @RESPONSE   1 B (short) of remaining PIN tries
     * @apiNote authentication not required
     */
    void unsecureGetPINTries(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        if (this.getAppletInitialized()) {
            ISOException.throwIt(RES_ERR_INITIALIZED);
        }

        getPINTries(apduBuffer);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, (short) 1);
    }

    /**
     * Secure get and send remaining PIN tries
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0
     * @RESPONSE   encrypted(remaining tries [1 B]) [16 B] | MAC [16 B]
     * @apiNote authentication not required
     */
    void secureGetPINTries(APDU apdu) {
        if (!this.getAppletInitialized()) {
            ISOException.throwIt(RES_ERR_UNINITIALIZED);
        }

        byte[] apduBuffer = apdu.getBuffer();
        secureChannel.decryptAPDU(apduBuffer);
        getPINTries(apduBuffer);
        short length = secureChannel.encryptResponse(apduBuffer, (short) 1, ISO7816.OFFSET_CDATA, RES_SUCCESS);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, length);
    }

    /**
     * Verify given PIN and set card into authenticated state
     * @param apduBuffer apdu buffer with PIN code of length PIN_MAX_LEN
     * @apiNote works in both uninitialized and initialized state
     */
    short verifyPIN(byte[] apduBuffer) {
        if(!this.pin.check(apduBuffer, ISO7816.OFFSET_CDATA, PIN_MAX_LEN)) {
            byte tries = this.pin.getTriesRemaining();
            if (tries == 0) {
                return RES_ERR_RESET;
            }
            this.setUserAuthenticated(false);
            return RES_ERR_NOT_LOGGED;
        }
        this.setUserAuthenticated(true);
        return RES_SUCCESS;
    }

    /**
     * Unsecure verify given PIN and set card into authenticated state.
     * When out of tries, card is reset to uninitialized default state.
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = 0x0A, DATA = PIN code
     * @RESPONSE   none
     * @apiNote authentication not required
     */
    void unsecureVerifyPIN(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();

        if (this.getAppletInitialized()) {
            ISOException.throwIt(RES_ERR_INITIALIZED);
        }

        short SW = verifyPIN(apduBuffer);
        if (SW == RES_ERR_RESET) {
            try {
                this.resetToDefault();
            } catch (Exception e) {
                ISOException.throwIt(RES_ERR_STORAGE);
            }
        }
        if (SW != RES_SUCCESS)
            ISOException.throwIt(SW);
    }

    /**
     * Secure verify given PIN and set card into authenticated state.
     * When out of tries, card is reset to uninitialized default state.
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = 0x20, DATA = encrypted PIN | MAC tag
     * @RESPONSE   encrypted response code | MAC tag
     * @apiNote authentication not required
     */
    void secureVerifyPIN(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();

        if (!this.getAppletInitialized()) {
            ISOException.throwIt(RES_ERR_UNINITIALIZED);
        }

        secureChannel.decryptAPDU(apduBuffer);
        short SW = verifyPIN(apduBuffer);
        if (SW == RES_ERR_RESET) {
            try {
                this.hardReset();
            } catch (Exception e) {
                SW = RES_ERR_STORAGE;
            }
        }
        secureChannel.encryptResponse(apduBuffer, (short) 0, ISO7816.OFFSET_CDATA, SW);
    }

    /**
     * Change current PIN to the PIN from APDU data
     *
     * @param apduBuffer buffer containing APDU command
     * @apiNote works in both uninitialized and initialized state
     */
    short changePIN(byte[] apduBuffer) {
        short dataLength = apduBuffer[ISO7816.OFFSET_LC];

        /* Check whether user is authenticated */
        if (!this.getUserAuthenticated()) {
            return RES_ERR_NOT_LOGGED;
        }
        /* Check that PIN has correct length = maximal length */
        // TODO: Add better check for PIN policy
        if (dataLength != PIN_MAX_LEN) {
            return RES_ERR_PIN_POLICY;
        }
        /* Set new user PIN */
        pin.update(apduBuffer, ISO7816.OFFSET_CDATA, (byte) dataLength);
        return RES_SUCCESS;
    }

    /**
     * Unsecure change current PIN to the PIN from APDU data
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = max 0x0A, DATA = PIN code
     * @RESPONSE   none
     * @apiNote authentication required
     */
    void unsecureChangePIN(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();
        if (this.getAppletInitialized()) {
            ISOException.throwIt(RES_ERR_INITIALIZED);
        }

        short SW = changePIN(apduBuffer);
        if (SW != RES_SUCCESS)
            ISOException.throwIt(SW);
    }

    /**
     * Secure change current PIN to the PIN from APDU data
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = max 0x10, DATA = encrypted(PIN code) [16 B] | MAC tag
     * @RESPONSE   none
     * @apiNote authentication required
     */
    void secureChangePIN(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();
        if (!this.getAppletInitialized()) {
            ISOException.throwIt(RES_ERR_UNINITIALIZED);
        }
        secureChannel.decryptAPDU(apduBuffer);
        short SW = changePIN(apduBuffer);
        secureChannel.encryptResponse(apduBuffer, (short) 0, ISO7816.OFFSET_CDATA, SW);
    }

    /**
     * Store secret into card filesystem with given access name
     * @param apduBuffer buffer with name and secret value
     * @APDU       P1 = 0, P2 = 0, L_c, DATA = name length [1 B] | name [max 10 B] | secret length [1 B] | secret [max 64 B]
     * @apiNote authentication required
     * @apiNote works in both uninitialized and initialized state
     */
    short storeSecret(byte[] apduBuffer) {
        /* Check whether user is authenticated */
        if (!this.getUserAuthenticated()) {
            return RES_ERR_NOT_LOGGED;
        }
        /* Create record in filesystem */
        try {
            byte nameLength = apduBuffer[ISO7816.OFFSET_CDATA];
            short nameOffset = 1 + ISO7816.OFFSET_CDATA;
            byte secretLength = apduBuffer[ISO7816.OFFSET_CDATA + 1 + nameLength];
            short secretOffset = (short) (1 + ISO7816.OFFSET_CDATA + 1 + nameLength);
            fileSystem.createRecord(apduBuffer, nameLength, nameOffset, secretLength, secretOffset);
        } catch (StorageException e) {
            return RES_ERR_STORAGE;
        } catch (InvalidArgumentException e) {
            return RES_ERR_SECRET_POLICY;
        }
        return RES_SUCCESS;
    }

    /**
     * Unsecure store secret into card filesystem with given access name
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c, DATA = name length [1 B] | name [max 10 B] | secret length [1 B] | secret [max 64 B]
     * @RESPONSE   none
     * @apiNote authentication required
     */
    void unsecureStoreSecret(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();
        if (this.getAppletInitialized()) {
            ISOException.throwIt(RES_ERR_INITIALIZED);
        }
        short SW = storeSecret(apduBuffer);
        if (SW != RES_SUCCESS)
            ISOException.throwIt(SW);
    }

    /**
     * Secure store secret into card filesystem with given access name
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c, DATA = name length [1 B] | name [max 10 B] | secret length [1 B] | secret [max 64 B]
     * @RESPONSE   none
     * @apiNote authentication required
     */
    void secureStoreSecret(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();
        if (!this.getAppletInitialized()) {
            ISOException.throwIt(RES_ERR_UNINITIALIZED);
        }

        secureChannel.decryptAPDU(apduBuffer);
        short SW = storeSecret(apduBuffer);
        secureChannel.encryptResponse(apduBuffer, (short) 0, ISO7816.OFFSET_CDATA, SW);
    }

    /**
     * Get names of all secrets stored in the card
     * @param apduBuffer buffer for storing all the names
     * @apiNote authentication not required
     * @apiNote works in both uninitialized and initialized state
     */
    private short getNames(byte[] apduBuffer) {
        /* User does not have to be authenticated */
        short namesLength;
        try {
            /* Get all names into the temporary buffer */
            namesLength = fileSystem.getAllNames(apduBuffer, ISO7816.OFFSET_CDATA);
            /* Copy result into response buffer*/
            apduBuffer[ISO7816.OFFSET_LC] = (byte) namesLength;
        } catch (StorageException e) {
            return RES_ERR_STORAGE;
        } catch (InvalidArgumentException e) {
            return RES_ERR_SECRET_POLICY;
        }
        return RES_SUCCESS;
    }

    /**
     * Unsecure get names of all secrets stored in the card
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = none, DATA = none
     * @RESPONSE   LV values of names concatenated together
     * @apiNote authentication not required
     */
    void unsecureGetNames(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        if (this.getAppletInitialized()) {
            ISOException.throwIt(RES_ERR_INITIALIZED);
        }

        short SW = getNames(apduBuffer);
        if (SW != RES_SUCCESS)
            ISOException.throwIt(SW);
        short namesLength = apduBuffer[ISO7816.OFFSET_LC];
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, namesLength);
    }

    /**
     * Secure get names of all secrets stored in the card
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = none, DATA = none
     * @RESPONSE   LV values of names concatenated together
     * @apiNote authentication not required
     */
    void secureGetNames(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        if (!this.getAppletInitialized()) {
            ISOException.throwIt(RES_ERR_UNINITIALIZED);
        }

        secureChannel.decryptAPDU(apduBuffer);
        short SW = getNames(apduBuffer);
        if (SW != RES_SUCCESS)
            ISOException.throwIt(SW);
        short namesLength = apduBuffer[ISO7816.OFFSET_LC];
        secureChannel.encryptResponse(apduBuffer, namesLength, ISO7816.OFFSET_CDATA, SW);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, namesLength);
    }

    /**
     * Delete secret of given name from filesystem
     * @param apduBuffer buffer with name
     * @apiNote authentication required
     * @apiNote works in both uninitialized and initialized state
     */
    private short deleteSecret(byte[] apduBuffer) {
        /* Check whether user is authenticated */
        if (!this.getUserAuthenticated()) {
            return RES_ERR_NOT_LOGGED;
        }
        /* Create record in filesystem */
        try {
            byte nameLength = apduBuffer[ISO7816.OFFSET_CDATA];
            short nameOffset = 1 + ISO7816.OFFSET_CDATA;
            fileSystem.deleteRecord(apduBuffer, nameLength, nameOffset);
        } catch (StorageException e) {
            return RES_ERR_STORAGE;
        } catch (InvalidArgumentException e) {
            return RES_ERR_SECRET_POLICY;
        }
        return RES_SUCCESS;
    }

    /**
     * Unsecure delete secret of given name
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = name length, DATA = name
     * @RESPONSE   none
     * @apiNote authentication required
     */
    void unsecureDeleteSecret(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        if (this.getAppletInitialized()) {
            ISOException.throwIt(RES_ERR_INITIALIZED);
        }
        short SW = deleteSecret(apduBuffer);
        if (SW != RES_SUCCESS)
            ISOException.throwIt(SW);
    }

    /**
     * Secure delete secret of given name
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = name length, DATA = name
     * @RESPONSE   none
     * @apiNote authentication required
     */
    void secureDeleteSecret(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        if (!this.getAppletInitialized()) {
            ISOException.throwIt(RES_ERR_UNINITIALIZED);
        }
        secureChannel.decryptAPDU(apduBuffer);
        short SW = deleteSecret(apduBuffer);
        secureChannel.encryptResponse(apduBuffer, (short) 0, ISO7816.OFFSET_CDATA, SW);
    }

    /**
     * Get secret of given name and copy it into apduBuffer
     * @param apduBuffer apdu command, data contains name of the secret
     * @apiNote authentication required
     * @apiNote works both by initialized and uninitialized mode
     */
    short getSecret(byte[] apduBuffer) {
        try {
            short secretLength = fileSystem.getSecretByName(apduBuffer, apduBuffer[ISO7816.OFFSET_LC],
                    ISO7816.OFFSET_CDATA, apduBuffer, ISO7816.OFFSET_CDATA);
            apduBuffer[ISO7816.OFFSET_LC] = (byte) secretLength;
        } catch (InvalidArgumentException e) {
            return RES_ERR_NAME_POLICY;
        } catch (StorageException e) {
            return RES_ERR_STORAGE;
        } catch (Exception e) {
            return RES_ERR_GENERAL;
        }
        return RES_SUCCESS;
    }

    /**
     * Unsecure get secret of given name
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = name length, DATA = name
     * @RESPONSE   secret value
     * @apiNote authentication required
     */
    void unsecureGetSecret(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        if (this.getAppletInitialized()) {
            ISOException.throwIt(RES_ERR_INITIALIZED);
        }
        short SW = getSecret(apduBuffer);
        if (SW != RES_SUCCESS)
            ISOException.throwIt(SW);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, apduBuffer[ISO7816.OFFSET_LC]);
    }

    /**
     * Secure get secret of given name
     * @param apdu apdu command
     * @APDU       P1 = 0, P2 = 0, L_c = name length, DATA = name
     * @RESPONSE   none
     * @apiNote authentication required
     */
    void secureGetSecret(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        if (!this.getAppletInitialized()) {
            ISOException.throwIt(RES_ERR_UNINITIALIZED);
        }

        secureChannel.decryptAPDU(apduBuffer);
        short SW = getSecret(apduBuffer);
        short encryptedLength = secureChannel.encryptResponse(apduBuffer, apduBuffer[ISO7816.OFFSET_LC], ISO7816.OFFSET_CDATA, SW);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, encryptedLength);
    }

    /**
     * Reset data on card to default state
     * It does not influence the initialization state itself
     */
    private void resetToDefault() throws StorageException {
        eraseSecretData();
        this.pin.reset();
        this.pin.update(DEFAULT_PIN, (short) 0, PIN_MAX_LEN);
        this.secureChannel.closeSecureChannel();
    }

    /**
     * Erase data from filesystem
     */
    private void eraseSecretData() throws StorageException {
        fileSystem.eraseData();
    }

    /**
     * Set card into default uninitialized state
     */
    private void hardReset() throws StorageException {
        this.secureChannel.eraseSecureChannel();
        this.resetToDefault();
    }
}
