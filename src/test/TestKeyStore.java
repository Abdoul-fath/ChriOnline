package test;

import java.security.PrivateKey;
import java.security.PublicKey;

import security.KeyStoreManager;
import security.Signer;
import security.Verifier;

public class TestKeyStore {

    public static void main(String[] args) {
        try {
            String keystorePath = "keys/monkeystore.p12";
            String password = "abdoul";
            String alias = "monalias";

            KeyStoreManager manager = new KeyStoreManager(keystorePath, password);

            PrivateKey privateKey = manager.getPrivateKey(alias);
            PublicKey publicKey = manager.getPublicKey(alias);

            String message = "Paiement de 250 DH";

            String signature = Signer.sign(message, privateKey);

            boolean valid = Verifier.verify(message, signature, publicKey);

            System.out.println("Message : " + message);
            System.out.println("Signature : " + signature);
            System.out.println("Signature valide ? " + valid);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}