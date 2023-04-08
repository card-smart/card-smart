package tests;

import cz.muni.fi.crocs.rcard.client.CardManager;
import cz.muni.fi.crocs.rcard.client.CardType;
import org.junit.jupiter.api.*;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class CardSmartStoreSecretTest extends BaseTest {
    private static final byte PIN_MAX_TRIES = (byte)5;
    public CardSmartStoreSecretTest() {
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

    /* Store secret */
    @Test
    public void storeSecret_4bytes() throws Exception {
        CardManager card = connect();
        /* Verify on default PIN */
        byte[] data = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
        CommandAPDU cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());

        /* Store secret on card */
        byte[] secretData = {4, 0x31, 0x32, 0x33, 0x34, 4, 1, 2, 3, 4};
        cmd = new CommandAPDU(0xB0, 0x25, 0x00, 0x00, secretData);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
    }

    @Test
    public void storeSecret_maxNameMaxSecret() throws Exception {
        CardManager card = connect();
        /* Verify on default PIN */
        byte[] data = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
        CommandAPDU cmd = new CommandAPDU(0xC0, 0x61, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());

        /* Store secret on card */
        byte[] secretData = {10, 0x20, 0x21, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x40, 0x0, 0x1, 0x2, 0x3,
                0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16,
                0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27,
                0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
                0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f};
        cmd = new CommandAPDU(0xC0, 0x80, 0x00, 0x00, secretData);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
    }

    @Test
    public void storeSecret_shortName() throws Exception {
        CardManager card = connect();
        /* Verify on default PIN */
        byte[] data = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
        CommandAPDU cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());

        /* Store secret on card */
        byte[] secretData = {3, 0x31, 0x32, 0x33, 4, 1, 2, 3, 4};
        cmd = new CommandAPDU(0xB0, 0x25, 0x00, 0x00, secretData);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B06, responseAPDU.getSW());
    }

    @Test
    public void storeSecret_overflowName() throws Exception {
        CardManager card = connect();
        /* Verify on default PIN */
        byte[] data = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
        CommandAPDU cmd = new CommandAPDU(0xC0, 0x61, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());

        /* Store secret on card */
        byte[] secretData = {11, 0x00, 0x01, 0x02,  0x03, 0x04, 0x05,  0x06, 0x07, 0x08,  0x09, 0x0A, 0x0B,
                4, 1, 2, 3, 4};
        cmd = new CommandAPDU(0xC0, 0x80, 0x00, 0x00, secretData);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B06, responseAPDU.getSW());
    }

    @Test
    public void storeSecret_overflowSecret() throws Exception {
        CardManager card = connect();
        /* Verify on default PIN */
        byte[] data = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
        CommandAPDU cmd = new CommandAPDU(0xC0, 0x61, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());

        /* Store secret on card */
        byte[] secretData = {4, 0x31, 0x32, 0x33, 0x34, 0x41, 0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xa,
                0xb, 0xc, 0xd, 0xe, 0xf, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c,
                0x1d, 0x1e, 0x1f, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d,
                0x2e, 0x2f, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e,
                0x3f};
        cmd = new CommandAPDU(0xC0, 0x80, 0x00, 0x00, secretData);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B06, responseAPDU.getSW());
    }

    @Test
    public void storeSecret_zeroSecret() throws Exception {
        CardManager card = connect();
        /* Verify on default PIN */
        byte[] data = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
        CommandAPDU cmd = new CommandAPDU(0xC0, 0x61, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());

        /* Store secret on card */
        byte[] secretData = {4, 0x31, 0x32, 0x33, 0x34, 0};
        cmd = new CommandAPDU(0xC0, 0x80, 0x00, 0x00, secretData);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B06, responseAPDU.getSW());
    }
}
