package tests;

import cz.muni.fi.crocs.rcard.client.CardType;
import org.junit.Assert;
import org.junit.jupiter.api.*;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/**
 * Example test class for the applet
 * Note: If simulator cannot be started try adding "-noverify" JVM parameter
 *
 * @author xsvenda, Dusan Klinec (ph4r05)
 */
public class CardSmartBasicTest extends BaseTest {
    
    public CardSmartBasicTest() {
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

    /* Unknown CLA byte */
    @Test
    public void wrongCLA() throws Exception {
        CommandAPDU cmd = new CommandAPDU(0xC1, 0x90, 0x00, 0x00);
        final ResponseAPDU responseAPDU = connect().transmit(cmd);
        Assert.assertNotNull(responseAPDU);
        Assert.assertEquals(0x6C00, responseAPDU.getSW());
        //Assert.assertNotNull(responseAPDU.getBytes());
    }

    /* Unknown INS byte */
    @Test
    public void unknownINS() throws Exception {
        CommandAPDU cmd = new CommandAPDU(0xC0, 0x10, 0x00, 0x00);
        final ResponseAPDU responseAPDU = connect().transmit(cmd);
        Assert.assertNotNull(responseAPDU);
        Assert.assertEquals(0x6C01, responseAPDU.getSW());
        //Assert.assertNotNull(responseAPDU.getBytes());
    }
}
