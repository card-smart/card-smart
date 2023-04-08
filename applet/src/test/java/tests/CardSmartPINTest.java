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
        CommandAPDU cmd = new CommandAPDU(0xB0, 0x21, 0x00, 0x00);
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
        CommandAPDU cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(0, responseAPDU.getData().length);

        /* Remaining tries should not change */
        cmd = new CommandAPDU(0xB0, 0x21, 0x00, 0x00);
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
        CommandAPDU cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(0, responseAPDU.getData().length);

        /* Remaining tries decrease */
        cmd = new CommandAPDU(0xB0, 0x21, 0x00, 0x00);
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
        CommandAPDU wrongPINCMD = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(wrongPINCMD);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(0, responseAPDU.getData().length);

        /* Remaining tries decrease */
        CommandAPDU cmd = new CommandAPDU(0xB0, 0x21, 0x00, 0x00);
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
        CommandAPDU cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(0, responseAPDU.getData().length);

        /* Remaining tries decrease */
        cmd = new CommandAPDU(0xB0, 0x21, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(1, responseAPDU.getData().length);
        Assertions.assertEquals(responseAPDU.getData()[0], PIN_MAX_TRIES - 1);

        /* Test verify on default PIN */
        byte[] correctPIN = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
        cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, correctPIN);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(0, responseAPDU.getData().length);

        /* Remaining tries should reset back */
        cmd = new CommandAPDU(0xB0, 0x21, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(1, responseAPDU.getData().length);
        Assertions.assertEquals(responseAPDU.getData()[0], PIN_MAX_TRIES);
    }

    /* Change PIN without logging in */
    @Test
    public void changePIN_notLogged() throws Exception {
        CardManager card = connect();
        /* Try to change PIN when not logged in */
        byte[] newPIN = {0x30, 0x30, 0x30, 0x31, 0, 0, 0, 0, 0, 0};
        CommandAPDU cmd = new CommandAPDU(0xB0, 0x23, 0x00, 0x00, newPIN);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());

        /* Remaining tries should stay unchanged */
        cmd = new CommandAPDU(0xB0, 0x21, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(1, responseAPDU.getData().length);
        Assertions.assertEquals(responseAPDU.getData()[0], PIN_MAX_TRIES);
    }

    @Test
    public void changePIN_short() throws Exception {
        CardManager card = connect();
        /* Test verify on default PIN */
        byte[] data = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
        CommandAPDU cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());

        /* Set new PIN which does not have correct length  */
        byte[] newPIN = {0x30, 0x30, 0x30, 0x31, 0, 0, 0};
        cmd = new CommandAPDU(0xB0, 0x23, 0x00, 0x00, newPIN);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B03, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());

        /* Remaining tries should stay unchanged */
        cmd = new CommandAPDU(0xB0, 0x21, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(1, responseAPDU.getData().length);
        Assertions.assertEquals(responseAPDU.getData()[0], PIN_MAX_TRIES);
    }

    @Test
    public void changePIN_long() throws Exception {
        CardManager card = connect();
        /* Test verify on default PIN */
        byte[] data = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
        CommandAPDU cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());

        /* Set new PIN which does not have correct length */
        byte[] newPIN = {0x30, 0x30, 0x30, 0x31, 0, 0, 0, 0, 0, 0, 0};
        cmd = new CommandAPDU(0xB0, 0x23, 0x00, 0x00, newPIN);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B03, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());

        /* Remaining tries should stay unchanged */
        cmd = new CommandAPDU(0xB0, 0x21, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(1, responseAPDU.getData().length);
        Assertions.assertEquals(responseAPDU.getData()[0], PIN_MAX_TRIES);
    }

    @Test
    public void changePIN_correct() throws Exception {
        CardManager card = connect();
        /* Test verify on default PIN */
        byte[] data = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
        CommandAPDU cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());

        /* Set new PIN which does not have correct length  */
        byte[] newPIN = {0x30, 0x30, 0x30, 0x31, 0, 0, 0, 0, 0, 0};
        cmd = new CommandAPDU(0xB0, 0x23, 0x00, 0x00, newPIN);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());

        /* Remaining tries should stay unchanged */
        cmd = new CommandAPDU(0xB0, 0x21, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(1, responseAPDU.getData().length);
        Assertions.assertEquals(responseAPDU.getData()[0], PIN_MAX_TRIES);

        /* Log again */
        cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, newPIN);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
    }

    @Test
    public void wrongPIN_resetTries() throws Exception {
        CardManager card = connect();
        /* Test verify on 1. wrong PIN */
        byte[] data = {0x30, 0x30, 0x30, 0x31, 0, 0, 0, 0, 0, 0};
        CommandAPDU cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(0, responseAPDU.getData().length);

        /* Test verify on 2. wrong PIN */
        cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(0, responseAPDU.getData().length);

        /* Test verify on 3. wrong PIN */
        cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(0, responseAPDU.getData().length);

        /* Test verify on 4. wrong PIN */
        cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(0, responseAPDU.getData().length);

        /* Remaining tries decrease */
        cmd = new CommandAPDU(0xB0, 0x21, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(1, responseAPDU.getData().length);
        Assertions.assertEquals(responseAPDU.getData()[0], PIN_MAX_TRIES - 4);

        /* Test verify on 5. wrong PIN -> reset Card */
        cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B02, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(0, responseAPDU.getData().length);

        /* Remaining tries reset */
        cmd = new CommandAPDU(0xB0, 0x21, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(1, responseAPDU.getData().length);
        Assertions.assertEquals(responseAPDU.getData()[0], PIN_MAX_TRIES);
    }

    @Test
    public void wrongPIN_resetCard() throws Exception {
        CardManager card = connect();
        /* Test verify on default PIN */
        byte[] pin = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
        CommandAPDU cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, pin);
        ResponseAPDU responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());

        /* Store secret on card */
        byte[] secretData = {4, 0x31, 0x32, 0x33, 0x34, 4, 1, 2, 3, 4};
        cmd = new CommandAPDU(0xB0, 0x25, 0x00, 0x00, secretData);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());

        /* Get all names from card*/
        cmd = new CommandAPDU(0xB0, 0x20, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertEquals(5, responseAPDU.getData().length);
        Assertions.assertArrayEquals(new byte[] {4, 0x31, 0x32, 0x33, 0x34}, responseAPDU.getData());

        /* Test verify on 1. wrong PIN */
        byte[] data = {0x30, 0x30, 0x30, 0x31, 0, 0, 0, 0, 0, 0};
        cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());

        /* Test verify on 2. wrong PIN */
        cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());

        /* Test verify on 3. wrong PIN */
        cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());

        /* Test verify on 4. wrong PIN */
        cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B01, responseAPDU.getSW());

        /* Test verify on 5. wrong PIN -> reset Card */
        cmd = new CommandAPDU(0xB0, 0x22, 0x00, 0x00, data);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x6B02, responseAPDU.getSW());

        /* Remaining tries reset */
        cmd = new CommandAPDU(0xB0, 0x21, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertNotNull(responseAPDU.getBytes());
        Assertions.assertEquals(1, responseAPDU.getData().length);
        Assertions.assertEquals(responseAPDU.getData()[0], PIN_MAX_TRIES);

        /* Get all names should be empty */
        cmd = new CommandAPDU(0xB0, 0x20, 0x00, 0x00);
        responseAPDU = card.transmit(cmd);
        Assertions.assertNotNull(responseAPDU);
        Assertions.assertEquals(0x9000, responseAPDU.getSW());
        Assertions.assertEquals(0, responseAPDU.getData().length);
    }
}
