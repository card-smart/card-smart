package main;

import applet.CardSmartApplet;
import applet.HelloWorldApplet;
import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;
import javacard.framework.AID;
import javacard.framework.Util;

import javax.smartcardio.*;

public class Run {
    static CardSimulator simulator;

    public static void main(String[] args){
        // 1. create simulator
         simulator = new CardSimulator();

        // 2. install applet
        AID appletAID = AIDUtil.create("F000000001");
        simulator.installApplet(appletAID, CardSmartApplet.class);

        // 3. select applet
        simulator.selectApplet(appletAID);

        // 4. send APDU
        {
            byte[] defaultPIN = {0x30, 0x30, 0x30, 0x30, 0, 0, 0, 0, 0, 0};
            byte[] newPIN = {0x31, 0x32, 0x33, 0x34, 0, 0, 0, 0, 0, 0};
            getTries();
            login(defaultPIN);
            changePIN(newPIN);
        }
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static void printAPDU(CommandAPDU apdu) {
        System.out.println("APDU: "
                + String.format(" %02X", apdu.getCLA())
                + String.format(" %02X", apdu.getINS())
                + String.format(" %02X", apdu.getP1())
                + String.format(" %02X", apdu.getP2())
                + String.format(" %02X ", apdu.getNc())
                + bytesToHex(apdu.getData()));
    }

    private static void printResponse(ResponseAPDU response) {
        int SW1 = response.getSW1();
        int SW2 = response.getSW2();
        System.out.println("RES: " + String.format("%02X", SW1) + " " + String.format("%02X", SW2));
        System.out.println("DATA: " + bytesToHex(response.getData()));
    }

    private static void getTries() {
        CommandAPDU commandAPDU = new CommandAPDU(0xC0, 0x60, 0x00, 0x00);
        ResponseAPDU response = simulator.transmitCommand(commandAPDU);
        printAPDU(commandAPDU);
        printResponse(response);
    }

    private static void login(byte[] pin) {
        CommandAPDU commandAPDU = new CommandAPDU(0xC0, 0x61, 0x00, 0x00, pin);
        ResponseAPDU response = simulator.transmitCommand(commandAPDU);
        printAPDU(commandAPDU);
        printResponse(response);
    }

    private static void changePIN(byte[] pin) {
        CommandAPDU commandAPDU = new CommandAPDU(0xC0, 0x62, 0x00, 0x00, pin);
        ResponseAPDU response = simulator.transmitCommand(commandAPDU);
        printAPDU(commandAPDU);
        printResponse(response);
    }

}