package tool;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.ECGenParameterSpec;

public class Secure {
    PublicKey publicKey;
    PrivateKey privateKey;

    public static byte[] createSecret(String path) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        try {
            FileOutputStream out = new FileOutputStream(path);
            out.write(bytes);
            out.close();
        } catch (IOException e) {
            System.out.println("The pairing secret could not be established");
            return null;
        }
        return bytes;
    }

    public void generateEC() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        // TODO catching exceptions
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
        keyGen.initialize(ecSpec, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
    }
}
