package tests;

import cz.muni.fi.crocs.rcard.client.CardManager;
import cz.muni.fi.crocs.rcard.client.CardType;
import org.junit.jupiter.api.*;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class CardSmartDeleteSecretTest extends BaseTest {
    public CardSmartDeleteSecretTest() {
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

    /* Delete secret */
    @Test
    public void deleteSecretFromCard() throws Exception {
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

        /* delete secret from card */
        byte[] name = {4, 0x31, 0x32, 0x33, 0x34};
        cmd = new CommandAPDU(0xB0, 0x26, 0x00, 0x00, name);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());

        /* Get names on card */
        cmd = new CommandAPDU(0xB0, 0x20, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertEquals(0, responseAPDU.getData().length);
    }

    /* Delete secret, no authentication */
    @Test
    public void deleteSecretFromCard_noAuthentication() throws Exception {
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

        /* Verify wrong PIN to log out */
        byte[] data2 = {0x3, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
        cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data2);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());

        /* delete secret from card */
        byte[] name = {4, 0x31, 0x32, 0x33, 0x34};
        cmd = new CommandAPDU(0xB0, 0x26, 0x00, 0x00, name);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());
    }
}
