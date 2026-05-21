package service;

import dao.UserDAO;
import model.User;
import security.KeystorePasswordManager;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.UUID;

import database.DatabaseConnection;

public class UserCertificateService {

    private static final String BASE_KEYS_DIR =
            "C:/Users/abdoo/eclipse-workspace/tp1/keys/users/";

    private static final String CA_KEYSTORE_PATH =
            "C:/Users/abdoo/eclipse-workspace/tp1/keys/ca-keystore.p12";
    private static final String CA_KEYSTORE_PASSWORD = "hisboula";
    private static final String CA_ALIAS = "chrionline-ca";

    private static final int VALIDITY_DAYS = 365;

    private final UserDAO userDAO;

    public UserCertificateService() {
        this.userDAO = new UserDAO();
    }

    public boolean createCertificateForUser(int userId) {
        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                System.out.println("Utilisateur introuvable");
                return false;
            }

            File dir = new File(BASE_KEYS_DIR);
            if (!dir.exists()) dir.mkdirs();

            String safeEmail    = user.getEmail()
                    .replace("@", "_at_").replace(".", "_");
            String alias        = "user_" + userId;
            String keystorePath = BASE_KEYS_DIR + safeEmail + "_keystore.p12";
            String csrPath      = BASE_KEYS_DIR + safeEmail + ".csr";
            String certPath     = BASE_KEYS_DIR + safeEmail + "_certificat.cer";
            String caCertPath   = BASE_KEYS_DIR + "ca_cert.cer";

            // ⭐ Mot de passe unique et aléatoire pour ce keystore
            String keystorePassword = UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 16);

            // ── ÉTAPE 1 : Générer paire de clés RSA ──────────────────────────
            System.out.println("Étape 1 : Génération de la paire de clés...");
            int step1 = new ProcessBuilder(
                    "keytool", "-genkeypair",
                    "-alias", alias,
                    "-keyalg", "RSA",
                    "-keysize", "2048",
                    "-keystore", keystorePath,
                    "-storetype", "PKCS12",
                    "-storepass", keystorePassword,
                    "-keypass", keystorePassword,
                    "-validity", String.valueOf(VALIDITY_DAYS),
                    "-dname", "CN=" + user.getEmail()
                            + ", OU=ChriOnline, O=ENSA, L=Tetouan, ST=Nord, C=MA"
            ).inheritIO().start().waitFor();

            if (step1 != 0) {
                System.out.println("Erreur étape 1 : génération keystore");
                return false;
            }

            // ── ÉTAPE 2 : Générer CSR ─────────────────────────────────────────
            System.out.println("Étape 2 : Génération de la CSR...");
            int step2 = new ProcessBuilder(
                    "keytool", "-certreq",
                    "-alias", alias,
                    "-keystore", keystorePath,
                    "-storetype", "PKCS12",
                    "-storepass", keystorePassword,
                    "-file", csrPath
            ).inheritIO().start().waitFor();

            if (step2 != 0) {
                System.out.println("Erreur étape 2 : génération CSR");
                return false;
            }

            // ── ÉTAPE 3 : CA signe la CSR ─────────────────────────────────────
            System.out.println("Étape 3 : Signature de la CSR par la CA...");
            int step3 = new ProcessBuilder(
                    "keytool", "-gencert",
                    "-alias", CA_ALIAS,
                    "-keystore", CA_KEYSTORE_PATH,
                    "-storetype", "PKCS12",
                    "-storepass", CA_KEYSTORE_PASSWORD,
                    "-infile", csrPath,
                    "-outfile", certPath,
                    "-validity", String.valueOf(VALIDITY_DAYS),
                    "-ext", "KeyUsage:critical=digitalSignature",
                    "-rfc"
            ).inheritIO().start().waitFor();

            if (step3 != 0) {
                System.out.println("Erreur étape 3 : signature CA");
                return false;
            }

            // ── ÉTAPE 4a : Exporter certificat CA ────────────────────────────
            System.out.println("Étape 4 : Import de la chaîne de confiance...");
            int step4a = new ProcessBuilder(
                    "keytool", "-exportcert",
                    "-alias", CA_ALIAS,
                    "-keystore", CA_KEYSTORE_PATH,
                    "-storetype", "PKCS12",
                    "-storepass", CA_KEYSTORE_PASSWORD,
                    "-file", caCertPath,
                    "-rfc"
            ).inheritIO().start().waitFor();

            if (step4a != 0) {
                System.out.println("Erreur étape 4a : export certificat CA");
                return false;
            }

            // ── ÉTAPE 4b : Importer CA dans keystore admin ───────────────────
            int step4b = new ProcessBuilder(
                    "keytool", "-importcert",
                    "-alias", CA_ALIAS,
                    "-keystore", keystorePath,
                    "-storetype", "PKCS12",
                    "-storepass", keystorePassword,
                    "-file", caCertPath,
                    "-noprompt"
            ).inheritIO().start().waitFor();

            if (step4b != 0) {
                System.out.println("Erreur étape 4b : import CA dans keystore admin");
                return false;
            }

            // ── ÉTAPE 5 : Importer certificat signé dans keystore admin ──────
            System.out.println("Étape 5 : Import du certificat signé...");
            int step5 = new ProcessBuilder(
                    "keytool", "-importcert",
                    "-alias", alias,
                    "-keystore", keystorePath,
                    "-storetype", "PKCS12",
                    "-storepass", keystorePassword,
                    "-file", certPath,
                    "-noprompt"
            ).inheritIO().start().waitFor();

            if (step5 != 0) {
                System.out.println("Erreur étape 5 : import certificat signé");
                return false;
            }

            // ── ÉTAPE 6 : Nettoyage CSR ──────────────────────────────────────
            new File(csrPath).delete();
            System.out.println("✅ Certificat signé par CA généré avec succès : "
                    + user.getEmail());

            // ── ÉTAPE 7 : Sauvegarder en base avec mot de passe chiffré ──────
            saveCertificateInfo(
                    user.getId(), user.getEmail(), user.getRole(),
                    alias, keystorePath, certPath, keystorePassword
            );

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void saveCertificateInfo(int userId, String email, String role,
                                     String alias, String keystorePath,
                                     String certPath,
                                     String keystorePassword) throws Exception {

        // ⭐ Chiffrer le mot de passe avant stockage
        String encryptedPassword = KeystorePasswordManager.encrypt(keystorePassword);

        String sql = """
                INSERT INTO user_certificates
                (user_id, email, role, alias_name, keystore_path,
                 certificate_path, status, expires_at, keystore_password_encrypted)
                VALUES (?, ?, ?, ?, ?, ?, 'active', ?, ?)
                ON DUPLICATE KEY UPDATE
                    keystore_path = VALUES(keystore_path),
                    certificate_path = VALUES(certificate_path),
                    status = 'active',
                    expires_at = VALUES(expires_at),
                    keystore_password_encrypted = VALUES(keystore_password_encrypted),
                    issued_at = NOW()
                """;

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(VALIDITY_DAYS);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, email);
            ps.setString(3, role);
            ps.setString(4, alias);
            ps.setString(5, keystorePath);
            ps.setString(6, certPath);
            ps.setTimestamp(7, java.sql.Timestamp.valueOf(expiresAt));
            ps.setString(8, encryptedPassword); // ⭐ mot de passe chiffré
            ps.executeUpdate();
        }
    }
}