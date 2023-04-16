package applet;

import javacard.security.ECKey;
import javacard.security.KeyPair;

/**
 * Custom parameters for SECP256r1 curve
 * @apiNote parameters taken from https://neuromancer.sk/std/secg/secp256r1
 */
public class SECP256r1 {
    static final byte[] SECP256r1_FP = {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff
    };
    static final byte[] SECP256r1_A = {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xfc
    };
    static final byte[] SECP256r1_B = {
            (byte) 0x5a, (byte) 0xc6, (byte) 0x35, (byte) 0xd8, (byte) 0xaa, (byte) 0x3a, (byte) 0x93, (byte) 0xe7,
            (byte) 0xb3, (byte) 0xeb, (byte) 0xbd, (byte) 0x55, (byte) 0x76, (byte) 0x98, (byte) 0x86, (byte) 0xbc,
            (byte) 0x65, (byte) 0x1d, (byte) 0x06, (byte) 0xb0, (byte) 0xcc, (byte) 0x53, (byte) 0xb0, (byte) 0xf6,
            (byte) 0x3b, (byte) 0xce, (byte) 0x3c, (byte) 0x3e, (byte) 0x27, (byte) 0xd2, (byte) 0x60, (byte) 0x4b
    };
    static final byte[] SECP256r1_G = {
            (byte) 0x04, (byte) 0x6b, (byte) 0x17, (byte) 0xd1, (byte) 0xf2, (byte) 0xe1, (byte) 0x2c, (byte) 0x42,
            (byte) 0x47, (byte) 0xf8, (byte) 0xbc, (byte) 0xe6, (byte) 0xe5, (byte) 0x63, (byte) 0xa4, (byte) 0x40,
            (byte) 0xf2, (byte) 0x77, (byte) 0x03, (byte) 0x7d, (byte) 0x81, (byte) 0x2d, (byte) 0xeb, (byte) 0x33, 
            (byte) 0xa0, (byte) 0xf4, (byte) 0xa1, (byte) 0x39, (byte) 0x45, (byte) 0xd8, (byte) 0x98, (byte) 0xc2, 
            (byte) 0x96, (byte) 0x4f, (byte) 0xe3, (byte) 0x42, (byte) 0xe2, (byte) 0xfe, (byte) 0x1a, (byte) 0x7f,
            (byte) 0x9b, (byte) 0x8e, (byte) 0xe7, (byte) 0xeb, (byte) 0x4a, (byte) 0x7c, (byte) 0x0f, (byte) 0x9e,
            (byte) 0x16, (byte) 0x2b, (byte) 0xce, (byte) 0x33, (byte) 0x57, (byte) 0x6b, (byte) 0x31, (byte) 0x5e,
            (byte) 0xce, (byte) 0xcb, (byte) 0xb6, (byte) 0x40, (byte) 0x68, (byte) 0x37, (byte) 0xbf, (byte) 0x51,
            (byte) 0xf5
    };
    static final byte[] SECP256r1_R = {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xbc, (byte) 0xe6, (byte) 0xfa, (byte) 0xad, (byte) 0xa7, (byte) 0x17, (byte) 0x9e, (byte) 0x84,
            (byte) 0xf3, (byte) 0xb9, (byte) 0xca, (byte) 0xc2, (byte) 0xfc, (byte) 0x63, (byte) 0x25, (byte) 0x51
    };

    static final byte SECP256r1_H = (byte)0x01;

    /**
     * Sets the SECP256r1 curve parameters to the given ECKeyPair
     *
     * @param keyPair the key where the curve parameters are set
     */
    static void setCurveParameters(KeyPair keyPair) {
        ECKey privateKey = (ECKey) keyPair.getPrivate();
        privateKey.setA(SECP256r1_A, (short) 0x00, (short) SECP256r1_A.length);
        privateKey.setB(SECP256r1_B, (short) 0x00, (short) SECP256r1_B.length);
        privateKey.setFieldFP(SECP256r1_FP, (short) 0x00, (short) SECP256r1_FP.length);
        privateKey.setG(SECP256r1_G, (short) 0x00, (short) SECP256r1_G.length);
        privateKey.setR(SECP256r1_R, (short) 0x00, (short) SECP256r1_R.length);
        privateKey.setK(SECP256r1_H);
        ECKey publicKey = (ECKey) keyPair.getPublic();
        publicKey.setA(SECP256r1_A, (short) 0x00, (short) SECP256r1_A.length);
        publicKey.setB(SECP256r1_B, (short) 0x00, (short) SECP256r1_B.length);
        publicKey.setFieldFP(SECP256r1_FP, (short) 0x00, (short) SECP256r1_FP.length);
        publicKey.setG(SECP256r1_G, (short) 0x00, (short) SECP256r1_G.length);
        publicKey.setR(SECP256r1_R, (short) 0x00, (short) SECP256r1_R.length);
        publicKey.setK(SECP256r1_H);
    }
}
