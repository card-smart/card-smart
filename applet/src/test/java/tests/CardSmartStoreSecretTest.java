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
        CommandAPDU cmd = new CommandAPDU(0xB0, 0x61, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());

        /* Store secret on card */
        byte[] secretData = {4, 0x31, 0x32, 0x33, 0x34, 4, 1, 2, 3, 4};
        cmd = new CommandAPDU(0xB0, 0x80, 0x00, 0x00, secretData);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
    }

    @Test
    public void storeSecret_shortName() throws Exception {
        CardManager card = connect();
        /* Verify on default PIN */
        byte[] data = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
        CommandAPDU cmd = new CommandAPDU(0xB0, 0x61, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());

        /* Store secret on card */
        byte[] secretData = {3, 0x31, 0x32, 0x33, 4, 1, 2, 3, 4};
        cmd = new CommandAPDU(0xB0, 0x80, 0x00, 0x00, secretData);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B06, responseAPDU.getSW());
    }
}
