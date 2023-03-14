package applet;

import javacard.framework.*;

public class CardSmartApplet extends Applet {

    /*
     * APDU Instruction Codes
     */
    /* CLA byte, specific for the applet */
    protected static final byte CLA_CARDSMARTAPPLET = (byte)0xC0;
    /* Unsecure Get Card EC Public Key */
    protected static final byte INS_GET_PUBLIC_KEY = (byte)0x40;
    /* Unsecure Open Secure Channel */
    protected static final byte INS_OPEN_SC = (byte)0x41;
    /* Unsecure Send Message */
    protected static final byte INS_MESSAGE = (byte)0x42;
    /* Unsecure Close Secure Channel */
    protected static final byte INS_CLOSE_SC = (byte)0x43;
    /* Secure Get Names Length */
    protected static final byte INS_NAMES_LEN = (byte)0x50;
    /* Secure Get Names */
    protected static final byte INS_GET_NAME = (byte)0x51;
    /* Secure Get PIN Remaining Tries */
    protected static final byte INS_PIN_TRIES = (byte)0x60;
    /* Secure PIN Verify */
    protected static final byte INS_PIN_VERIFY = (byte)0x61;
    /* Secure Change PIN */
    protected static final byte INS_PIN_CHANGE = (byte)0x62;
    /* Secure Get Length of Secret */
    protected static final byte INS_SECRET_LEN = (byte)0x70;
    /* Secure Get Value of Secret */
    protected static final byte INS_GET_SECRET = (byte)0x71;
    /* Secure Store Value of Secret */
    protected static final byte INS_STORE_SECRET = (byte)0x80;
    /* Secure Delete Secret */
    protected static final byte INS_DELETE_SECRET = (byte)0x81;

    /*
     * Response Codes
     */
    protected static final short RES_SUCCESS = (short)0x9000;
    protected static final short RES_ERR_DECRYPTION = (short)0x6A00;
    protected static final short RES_ERR_MAC = (short)0x6A01;
    protected static final short RES_ERR_GENERAL = (short)0x6B00;
    protected static final short RES_ERR_NOT_LOGGED = (short)0x6B01;
    protected static final short RES_ERR_RESET = (short)0x6B02;
    protected static final short RES_ERR_PIN_POLICY = (short)0x6B03;
    protected static final short RES_ERR_STORAGE_FULL = (short)0x6B04;
    protected static final short RES_ERR_NAME_POLICY = (short)0x6B05;
    protected static final short RES_ERR_SECRET_POLICY = (short)0x6B06;
    protected static final short RES_ERR_NO_DATA = (short)0x6B07;
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
    protected static final byte SECRET_MAX_LEN = (byte)64;

    /* Cryptographic instances */
    private OwnerPIN pin = null;

    /*
     * Other instances
     */
    FileSystem fileSystem;
    private static final short TEMP_ARRAY_LEN = (short) 256;
    private byte[] tempArray = null;
    private boolean[] isUserAuthenticated = null;

    public CardSmartApplet(byte[] bArray, short bOffset, byte bLength) {

        /* Create temporary array */
        this.tempArray = JCSystem.makeTransientByteArray(TEMP_ARRAY_LEN, JCSystem.CLEAR_ON_DESELECT);

        /* Set initial PIN */
        this.pin = new OwnerPIN(PIN_MAX_TRIES, PIN_MAX_LEN); // 5 tries, max 10 digits in pin
        this.pin.update(DEFAULT_PIN, (short) 0, PIN_MAX_LEN);

        /* Create array for user authentication */
        this.isUserAuthenticated = JCSystem.makeTransientBooleanArray((short) 1, JCSystem.CLEAR_ON_DESELECT);

        /* Initialize filesystem with empty records */
        this.fileSystem = new FileSystem();

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
                    case INS_PIN_TRIES:
                        this.getPINTries(apdu);
                        break;
                    case INS_PIN_VERIFY:
                        this.verifyPIN(apdu);
                        break;
                    case INS_PIN_CHANGE:
                        this.changePIN(apdu);
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

    void getPINTries(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();

        byte tries = pin.getTriesRemaining();
        tempArray[0] = tries;
        Util.arrayCopyNonAtomic(this.tempArray, (short) 0, apduBuffer, ISO7816.OFFSET_CDATA, (short) 1);

        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, (short) 1);
    }

    void verifyPIN(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();

        /* Verify pin*/
        if(!this.pin.check(apduBuffer, ISO7816.OFFSET_CDATA, PIN_MAX_LEN)) {
            byte tries = this.pin.getTriesRemaining();
            if (tries == 0) {
                this.resetSecretData();
            }
            this.setUserAuthenticated(false);
            ISOException.throwIt(RES_ERR_NOT_LOGGED);
        }
        this.setUserAuthenticated(true);
    }

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

    private void setUserAuthenticated(boolean isAuthenticated) {
        this.isUserAuthenticated[0] = isAuthenticated;
    }

    private boolean getUserAuthenticated() {
        return this.isUserAuthenticated[0];
    }

    private void resetSecretData() {
        //Util.arrayFillNonAtomic(tempArray, (short) 0, (short) TEMP_ARRAY_LEN, (byte) 0);
        eraseSecretData();
        this.pin.reset();
        this.pin.update(DEFAULT_PIN, (short) 0, PIN_MAX_LEN);
    }

    private void eraseSecretData() {
        // TODO
    }
}
