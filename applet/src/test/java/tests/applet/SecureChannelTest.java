package tests.applet;

import cz.muni.fi.crocs.rcard.client.CardManager;
import cz.muni.fi.crocs.rcard.client.CardType;
import org.junit.jupiter.api.*;
import tool.ToolSecureChannel;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class SecureChannelTest extends BaseTest {

    public SecureChannelTest() {
        // Change card type here if you want to use physical card
        setCardType(CardType.JCARDSIMLOCAL);
    }

    @BeforeAll
    public static void setUpClass() throws Exception {
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
    }

    @BeforeEach
    public void setUpMethod() throws Exception {
    }

    @AfterEach
    public void tearDownMethod() throws Exception {
    }

    /* Get public key of card */
    @Test
    public void getPublicKey() throws Exception {
        CardManager card = connect();
        CommandAPDU cmd = new CommandAPDU(0xB0, 0x40, 0x00, 0x00);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertEquals(67, responseAPDU.getBytes().length);

        ResponseAPDU secondResponseAPDU = card.transmit(cmd);
        Assertions.assertArrayEquals(responseAPDU.getBytes(), secondResponseAPDU.getBytes());
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertEquals(65, responseAPDU.getData().length);
        Assertions.assertEquals(0x04, responseAPDU.getData()[0]); // uncompressed point format
    }

    @Test
    public void convertKeyBytes() throws Exception {
        ToolSecureChannel secure = new ToolSecureChannel();

        byte[] publicKeyBytes = secure.getFreshPublicKeyBytes();
        Assertions.assertEquals(65, publicKeyBytes.length);
        Assertions.assertEquals(0x04, publicKeyBytes[0]);
    }

    @Test
    public void uninitOpenSecureChannel() throws Exception {
        ToolSecureChannel secure = new ToolSecureChannel();
        CardManager card = connect();

        byte[] publicKeyBytes = secure.getFreshPublicKeyBytes();

        CommandAPDU cmd = new CommandAPDU(0xB0, 0x42, 0x00, 0x00, publicKeyBytes);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertEquals(0x6A04, responseAPDU.getSW());
    }

    // tool always pads PIN
    /*@Test
    public void initWrongPINLength() throws Exception {
        ToolSecureChannel secure = new ToolSecureChannel();
        CardManager card = connect();

        CommandAPDU cmd = new CommandAPDU(0xB0, 0x40, 0x00, 0x00);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        byte[] cardPublicKeyBytes = responseAPDU.getData();

        try {
            byte[] PIN = {1, 2, 3, 4, 5, 6, 7, 8, 9};
            byte[] pairingSecret = new byte[32];
            byte[] payload = secure.prepareInitializationPayload(cardPublicKeyBytes, PIN, pairingSecret);
            assert(false);
        } catch (Exception e) {
            assert(true);
        }
    }*/

    @Test
    public void initWrongPairingSecretLength() throws Exception {
        ToolSecureChannel secure = new ToolSecureChannel();
        CardManager card = connect();

        CommandAPDU cmd = new CommandAPDU(0xB0, 0x40, 0x00, 0x00);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        byte[] cardPublicKeyBytes = responseAPDU.getData();

        try {
            byte[] PIN = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
            byte[] pairingSecret = new byte[33];
            byte[] payload = secure.prepareInitializationPayload(cardPublicKeyBytes, PIN, pairingSecret);
            assert(false);
        } catch (Exception e) {
            assert(true);
        }
    }

    @Test
    public void initWrongPoint() throws Exception {
        ToolSecureChannel secure = new ToolSecureChannel();
        CardManager card = connect();

        CommandAPDU cmd = new CommandAPDU(0xB0, 0x40, 0x00, 0x00);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        byte[] cardPublicKeyBytes = responseAPDU.getData();

        try {
            byte[] PIN = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
            byte[] pairingSecret = new byte[32];
            byte[] payload = secure.prepareInitializationPayload(cardPublicKeyBytes, PIN, pairingSecret);
            payload[0] = 0x05;
            cmd = new CommandAPDU(0xB0, 0x41, 0x00, 0x00, payload);
            responseAPDU = card.transmit(cmd);
            Assertions.assertEquals(0x6B07, responseAPDU.getSW());
        } catch (Exception e) {
            assert(false);
        }
    }

    @Test
    public void initCorrect() throws Exception {
        ToolSecureChannel secure = new ToolSecureChannel();
        CardManager card = connect();

        CommandAPDU cmd = new CommandAPDU(0xB0, 0x40, 0x00, 0x00);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        byte[] cardPublicKeyBytes = responseAPDU.getData();

        try {
            byte[] PIN = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
            byte[] pairingSecret = new byte[32];
            byte[] payload = secure.prepareInitializationPayload(cardPublicKeyBytes, PIN, pairingSecret);
            cmd = new CommandAPDU(0xB0, 0x41, 0x00, 0x00, payload);
            responseAPDU = card.transmit(cmd);
            Assertions.assertEquals(0x9000, responseAPDU.getSW());
        } catch (Exception e) {
            assert(false);
        }
        // unsecure get all names should fail
        cmd = new CommandAPDU(0xB0, 0x20, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertEquals(0x6A05, responseAPDU.getSW());
    }

    @Test
    public void openSecureChannel() throws Exception {
        ToolSecureChannel secure = new ToolSecureChannel();
        CardManager card = connect();

        CommandAPDU cmd = new CommandAPDU(0xB0, 0x40, 0x00, 0x00);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        byte[] cardPublicKeyBytes = responseAPDU.getData();

        try {
            byte[] PIN = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
            byte[] pairingSecret = new byte[32];
            byte[] payload = secure.prepareInitializationPayload(cardPublicKeyBytes, PIN, pairingSecret);
            cmd = new CommandAPDU(0xB0, 0x41, 0x00, 0x00, payload);
            responseAPDU = card.transmit(cmd);
        } catch (Exception e) {
            assert(false);
        }

        byte[] publicKeyBytes = secure.getFreshPublicKeyBytes();
        cmd = new CommandAPDU(0xB0, 0x42, 0x00, 0x00, publicKeyBytes);
        responseAPDU = card.transmit(cmd);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertEquals(48, responseAPDU.getData().length);
    }

    @Test
    public void verifyInSecureChannel() throws Exception {
        byte[] PIN = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        byte[] pairingSecret = new byte[32];
        ToolSecureChannel secure = new ToolSecureChannel();
        CardManager card = connect();

        CommandAPDU cmd = new CommandAPDU(0xB0, 0x40, 0x00, 0x00);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        byte[] cardPublicKeyBytes = responseAPDU.getData();

        try {
            byte[] payload = secure.prepareInitializationPayload(cardPublicKeyBytes, PIN, pairingSecret);
            cmd = new CommandAPDU(0xB0, 0x41, 0x00, 0x00, payload);
            responseAPDU = card.transmit(cmd);
        } catch (Exception e) {
            assert(false);
        }

        // get fresh card key
        cmd = new CommandAPDU(0xB0, 0x40, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        cardPublicKeyBytes = responseAPDU.getData();
        byte[] publicKeyBytes = secure.getFreshPublicKeyBytes();
        cmd = new CommandAPDU(0xB0, 0x42, 0x00, 0x00, publicKeyBytes);
        responseAPDU = card.transmit(cmd);
        secure.createSharedSecrets(pairingSecret, cardPublicKeyBytes, responseAPDU.getData());

        cmd = secure.prepareSecureAPDU((byte) 0xB0, (byte) 0x32, PIN);
        responseAPDU = card.transmit(cmd);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        byte[] response = secure.getResponseData(responseAPDU.getData());
        Assertions.assertArrayEquals(new byte[]{-112, 0}, response);
    }

    @Test
    public void uninitAfterWrongPINs() throws Exception {
        byte[] PIN = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        byte[] pairingSecret = new byte[32];
        ToolSecureChannel secure = new ToolSecureChannel();
        CardManager card = connect();

        CommandAPDU cmd = new CommandAPDU(0xB0, 0x40, 0x00, 0x00);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        byte[] cardPublicKeyBytes = responseAPDU.getData();

        try {
            byte[] payload = secure.prepareInitializationPayload(cardPublicKeyBytes, PIN, pairingSecret);
            cmd = new CommandAPDU(0xB0, 0x41, 0x00, 0x00, payload);
            responseAPDU = card.transmit(cmd);
        } catch (Exception e) {
            assert(false);
        }

        // get fresh card key
        cmd = new CommandAPDU(0xB0, 0x40, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        cardPublicKeyBytes = responseAPDU.getData();
        byte[] publicKeyBytes = secure.getFreshPublicKeyBytes();
        cmd = new CommandAPDU(0xB0, 0x42, 0x00, 0x00, publicKeyBytes);
        responseAPDU = card.transmit(cmd);
        secure.createSharedSecrets(pairingSecret, cardPublicKeyBytes, responseAPDU.getData());

        // 1. attempt
        byte[] wrongPIN = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0};
        cmd = secure.prepareSecureAPDU((byte) 0xB0, (byte) 0x32, wrongPIN);
        responseAPDU = card.transmit(cmd);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        byte[] response = secure.getResponseData(responseAPDU.getData());
        Assertions.assertArrayEquals(new byte[]{107, 1}, response); // not logged in

        // 2. attempt
        cmd = secure.prepareSecureAPDU((byte) 0xB0, (byte) 0x32, wrongPIN);
        responseAPDU = card.transmit(cmd);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        response = secure.getResponseData(responseAPDU.getData());
        Assertions.assertArrayEquals(new byte[]{107, 1}, response); // not logged in

        // 3. attempt
        cmd = secure.prepareSecureAPDU((byte) 0xB0, (byte) 0x32, wrongPIN);
        responseAPDU = card.transmit(cmd);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        response = secure.getResponseData(responseAPDU.getData());
        Assertions.assertArrayEquals(new byte[]{107, 1}, response); // not logged in

        // 4. attempt
        cmd = secure.prepareSecureAPDU((byte) 0xB0, (byte) 0x32, wrongPIN);
        responseAPDU = card.transmit(cmd);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        response = secure.getResponseData(responseAPDU.getData());
        Assertions.assertArrayEquals(new byte[]{107, 1}, response); // not logged in

        // 5. attempt
        cmd = secure.prepareSecureAPDU((byte) 0xB0, (byte) 0x32, wrongPIN);
        responseAPDU = card.transmit(cmd);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        response = secure.getResponseData(responseAPDU.getData());
        Assertions.assertArrayEquals(new byte[]{107, 2}, response); // card reset

        // card should be reset into uninitialized state
        cmd = secure.prepareSecureAPDU((byte) 0xB0, (byte) 0x32, wrongPIN);
        responseAPDU = card.transmit(cmd);
        Assertions.assertEquals(0x6A04, responseAPDU.getSW());
    }
}
