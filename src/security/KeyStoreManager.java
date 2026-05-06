package security;

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

public class KeyStoreManager {

    private KeyStore keyStore;
    private final String keystorePath;
    private final char[] password;

    public KeyStoreManager(String keystorePath, String password) throws Exception {
        this.keystorePath = keystorePath;
        this.password = password.toCharArray();
        loadKeyStore();
    }

    private void loadKeyStore() throws Exception {
        keyStore = KeyStore.getInstance("PKCS12");

        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, password);
        }
    }

    public PrivateKey getPrivateKey(String alias) throws Exception {
        Key key = keyStore.getKey(alias, password);

        if (key instanceof PrivateKey) {
            return (PrivateKey) key;
        }

        throw new Exception("Aucune clé privée trouvée pour l'alias : " + alias);
    }

    public PublicKey getPublicKey(String alias) throws Exception {
        Certificate cert = keyStore.getCertificate(alias);

        if (cert != null) {
            return cert.getPublicKey();
        }

        throw new Exception("Aucun certificat trouvé pour l'alias : " + alias);
    }

    public Certificate getCertificate(String alias) throws Exception {
        return keyStore.getCertificate(alias);
    }
}