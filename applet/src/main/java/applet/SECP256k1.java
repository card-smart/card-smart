package applet;

import javacard.security.ECKey;
import javacard.security.KeyPair;

/**
 * Custom parameters for SECP256k1 curve
 * @apiNote parameters taken from Keycard project
 * @link https://github.com/status-im/status-keycard
 */
public class SECP256k1 {
    static final byte[] SECP256K1_FP = {
            (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
            (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
            (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
            (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFE,(byte)0xFF,(byte)0xFF,(byte)0xFC,(byte)0x2F
    };
    static final byte[] SECP256K1_A = {
            (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
            (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
            (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
            (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
    };
    static final byte[] SECP256K1_B = {
            (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
            (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
            (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
            (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x07
    };
    static final byte[] SECP256K1_G = {
            (byte)0x04,
            (byte)0x79,(byte)0xBE,(byte)0x66,(byte)0x7E,(byte)0xF9,(byte)0xDC,(byte)0xBB,(byte)0xAC,
            (byte)0x55,(byte)0xA0,(byte)0x62,(byte)0x95,(byte)0xCE,(byte)0x87,(byte)0x0B,(byte)0x07,
            (byte)0x02,(byte)0x9B,(byte)0xFC,(byte)0xDB,(byte)0x2D,(byte)0xCE,(byte)0x28,(byte)0xD9,
            (byte)0x59,(byte)0xF2,(byte)0x81,(byte)0x5B,(byte)0x16,(byte)0xF8,(byte)0x17,(byte)0x98,
            (byte)0x48,(byte)0x3A,(byte)0xDA,(byte)0x77,(byte)0x26,(byte)0xA3,(byte)0xC4,(byte)0x65,
            (byte)0x5D,(byte)0xA4,(byte)0xFB,(byte)0xFC,(byte)0x0E,(byte)0x11,(byte)0x08,(byte)0xA8,
            (byte)0xFD,(byte)0x17,(byte)0xB4,(byte)0x48,(byte)0xA6,(byte)0x85,(byte)0x54,(byte)0x19,
            (byte)0x9C,(byte)0x47,(byte)0xD0,(byte)0x8F,(byte)0xFB,(byte)0x10,(byte)0xD4,(byte)0xB8
    };
    static final byte[] SECP256K1_R = {
            (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
            (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFE,
            (byte)0xBA,(byte)0xAE,(byte)0xDC,(byte)0xE6,(byte)0xAF,(byte)0x48,(byte)0xA0,(byte)0x3B,
            (byte)0xBF,(byte)0xD2,(byte)0x5E,(byte)0x8C,(byte)0xD0,(byte)0x36,(byte)0x41,(byte)0x41
    };

    static final byte SECP256K1_K = (byte)0x01;

    /**
     * Sets the SECP256k1 curve parameters to the given ECKeyPair
     *
     * @param keyPair the key where the curve parameters are set
     */
    public static void setCurveParameters(KeyPair keyPair) {
        ECKey privateKey = (ECKey) keyPair.getPrivate();
        privateKey.setA(SECP256K1_A, (short) 0x00, (short) SECP256K1_A.length);
        privateKey.setB(SECP256K1_B, (short) 0x00, (short) SECP256K1_B.length);
        privateKey.setFieldFP(SECP256K1_FP, (short) 0x00, (short) SECP256K1_FP.length);
        privateKey.setG(SECP256K1_G, (short) 0x00, (short) SECP256K1_G.length);
        privateKey.setR(SECP256K1_R, (short) 0x00, (short) SECP256K1_R.length);
        privateKey.setK(SECP256K1_K);
        ECKey publicKey = (ECKey) keyPair.getPublic();
        publicKey.setA(SECP256K1_A, (short) 0x00, (short) SECP256K1_A.length);
        publicKey.setB(SECP256K1_B, (short) 0x00, (short) SECP256K1_B.length);
        publicKey.setFieldFP(SECP256K1_FP, (short) 0x00, (short) SECP256K1_FP.length);
        publicKey.setG(SECP256K1_G, (short) 0x00, (short) SECP256K1_G.length);
        publicKey.setR(SECP256K1_R, (short) 0x00, (short) SECP256K1_R.length);
        publicKey.setK(SECP256K1_K);
    }
}
