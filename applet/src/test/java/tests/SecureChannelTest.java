package tests;

import cz.muni.fi.crocs.rcard.client.CardManager;
import cz.muni.fi.crocs.rcard.client.CardType;
import org.junit.jupiter.api.*;

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
        CommandAPDU cmd = new CommandAPDU(0xC0, 0x40, 0x00, 0x00);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertEquals(67, responseAPDU.getBytes().length);

        ResponseAPDU secondResponseAPDU = card.transmit(cmd);
        Assertions.assertArrayEquals(responseAPDU.getBytes(), secondResponseAPDU.getBytes());
    }
}
