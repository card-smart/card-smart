package tests;

import cz.muni.fi.crocs.rcard.client.CardManager;
import cz.muni.fi.crocs.rcard.client.CardType;
import org.junit.Assert;
import org.junit.jupiter.api.*;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class CardSmartPINTest extends BaseTest {
    private static final byte PIN_MAX_TRIES = (byte)5;
    public CardSmartPINTest() {
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

    /* PIN remaining tries */
    @Test
    public void remainingTries() throws Exception {
        CommandAPDU cmd = new CommandAPDU(0xC0, 0x60, 0x00, 0x00);
        final ResponseAPDU responseAPDU = connect().transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(1, responseAPDU.getData().length);
        Assertions.assertEquals(responseAPDU.getData()[0], PIN_MAX_TRIES);
    }

    /* PIN remaining tries */
    @Test
    public void defaultPIN_allTries() throws Exception {
        CardManager card = connect();
        /* Test verify on default PIN */
        byte[] data = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
        CommandAPDU cmd = new CommandAPDU(0xC0, 0x61, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(0, responseAPDU.getData().length);

        /* Remainig tries should not change */
        cmd = new CommandAPDU(0xC0, 0x60, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(1, responseAPDU.getData().length);
        Assertions.assertEquals(responseAPDU.getData()[0], PIN_MAX_TRIES);
    }

    @Test
    public void wrongPIN_lessTries() throws Exception {
        CardManager card = connect();
        /* Test verify on wrong PIN */
        byte[] data = {0x30, 0x30, 0x30, 0x31, 0, 0, 0, 0, 0, 0};
        CommandAPDU cmd = new CommandAPDU(0xC0, 0x61, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(0, responseAPDU.getData().length);

        /* Remaining tries decrease */
        cmd = new CommandAPDU(0xC0, 0x60, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(1, responseAPDU.getData().length);
        Assertions.assertEquals(responseAPDU.getData()[0], PIN_MAX_TRIES - 1);
    }

    @Test
    public void twoWrongPINs_lessTries() throws Exception {
        CardManager card = connect();
        /* Test verify on wrong PIN */
        byte[] data = {0x30, 0x30, 0x30, 0x31, 0, 0, 0, 0, 0, 0};
        CommandAPDU wrongPINCMD = new CommandAPDU(0xC0, 0x61, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(wrongPINCMD);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(0, responseAPDU.getData().length);

        /* Remaining tries decrease */
        CommandAPDU cmd = new CommandAPDU(0xC0, 0x60, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(1, responseAPDU.getData().length);
        Assertions.assertEquals(responseAPDU.getData()[0], PIN_MAX_TRIES - 1);

        /* Second test verify on wrong PIN */
        responseAPDU = card.transmit(wrongPINCMD);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(0, responseAPDU.getData().length);

        /* Remaining tries decrease */
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(1, responseAPDU.getData().length);
        Assertions.assertEquals(responseAPDU.getData()[0], PIN_MAX_TRIES - 2);
    }

    @Test
    public void wrongPIN_correctPIN() throws Exception {
        CardManager card = connect();
        /* Test verify on wrong PIN */
        byte[] data = {0x30, 0x30, 0x30, 0x31, 0, 0, 0, 0, 0, 0};
        CommandAPDU cmd = new CommandAPDU(0xC0, 0x61, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(0, responseAPDU.getData().length);

        /* Remaining tries decrease */
        cmd = new CommandAPDU(0xC0, 0x60, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(1, responseAPDU.getData().length);
        Assertions.assertEquals(responseAPDU.getData()[0], PIN_MAX_TRIES - 1);

        /* Test verify on default PIN */
        byte[] correctPIN = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
        cmd = new CommandAPDU(0xC0, 0x61, 0x00, 0x00, correctPIN);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(0, responseAPDU.getData().length);

        /* Remainig tries should reset back */
        cmd = new CommandAPDU(0xC0, 0x60, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(1, responseAPDU.getData().length);
        Assertions.assertEquals(responseAPDU.getData()[0], PIN_MAX_TRIES);
    }
}
