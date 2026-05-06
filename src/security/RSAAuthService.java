package security;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class RSAAuthService {

    private KeyStoreManager keyStoreManager;
    private String alias;

    public RSAAuthService(String keystorePath, String password, String alias) throws Exception {
        this.keyStoreManager = new KeyStoreManager(keystorePath, password);
        this.alias = alias;
    }

    public String signerDonnee(String data) throws Exception {
        PrivateKey privateKey = keyStoreManager.getPrivateKey(alias);
        return Signer.sign(data, privateKey);
    }

    public String signerChallenge(String challenge) throws Exception {
        return signerDonnee(challenge);
    }

    // =========================================================
    // Vérification simple avec certificat utilisateur
    // =========================================================

    public static boolean verifierAvecCertificat(String data, String signatureBase64, String certificatPath) throws Exception {
        PublicKey publicKey = chargerClePubliqueDepuisCertificat(certificatPath);
        return Verifier.verify(data, signatureBase64, publicKey);
    }

    // =========================================================
    // Vérification complète : certificat + truststore CA
    // =========================================================

    public static boolean verifierAvecTruststore(String data,
                                                 String signatureBase64,
                                                 String certificatPath,
                                                 String truststorePath,
                                                 String truststorePassword,
                                                 String caAlias) throws Exception {

        X509Certificate userCert = chargerCertificatX509(certificatPath);

        // 1. Vérifier expiration du certificat utilisateur
        userCert.checkValidity();

        // 2. Charger truststore serveur
        KeyStore trustStore = KeyStore.getInstance("PKCS12");

        try (FileInputStream fis = new FileInputStream(truststorePath)) {
            trustStore.load(fis, truststorePassword.toCharArray());
        }

        // 3. Récupérer certificat CA
        Certificate caCertificate = trustStore.getCertificate(caAlias);

        if (caCertificate == null) {
            throw new Exception("Certificat CA introuvable dans le truststore : " + caAlias);
        }

        PublicKey caPublicKey = caCertificate.getPublicKey();

        // 4. Vérifier que le certificat utilisateur est signé par la CA
        userCert.verify(caPublicKey);

        // 5. Vérifier la signature RSA du challenge
        PublicKey userPublicKey = userCert.getPublicKey();

        return Verifier.verify(data, signatureBase64, userPublicKey);
    }

    private static PublicKey chargerClePubliqueDepuisCertificat(String certificatPath) throws Exception {
        X509Certificate certificat = chargerCertificatX509(certificatPath);
        certificat.checkValidity();
        return certificat.getPublicKey();
    }

    private static X509Certificate chargerCertificatX509(String certificatPath) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        try (FileInputStream fis = new FileInputStream(certificatPath)) {
            return (X509Certificate) factory.generateCertificate(fis);
        }
    }
}