package service;

import dao.UserDAO;
import model.User;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;

import database.DatabaseConnection;

public class UserCertificateService {

    private static final String BASE_KEYS_DIR =
            "C:/Users/abdoo/eclipse-workspace/tp1/keys/users/";

    private static final String STORE_PASSWORD = "123456"; // temporaire pour test
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
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String safeEmail = user.getEmail().replace("@", "_at_").replace(".", "_");
            String alias = "user_" + userId;
            String keystorePath = BASE_KEYS_DIR + safeEmail + "_keystore.p12";
            String certPath = BASE_KEYS_DIR + safeEmail + "_certificat.cer";

            // 1. Générer keystore PKCS12
            ProcessBuilder genKey = new ProcessBuilder(
                    "keytool",
                    "-genkeypair",
                    "-alias", alias,
                    "-keyalg", "RSA",
                    "-keysize", "2048",
                    "-keystore", keystorePath,
                    "-storetype", "PKCS12",
                    "-storepass", STORE_PASSWORD,
                    "-keypass", STORE_PASSWORD,
                    "-validity", String.valueOf(VALIDITY_DAYS),
                    "-dname", "CN=" + user.getEmail() + ", OU=ChriOnline, O=ENSA, L=Tetouan, ST=Nord, C=MA"
            );

            genKey.inheritIO();
            int genResult = genKey.start().waitFor();

            if (genResult != 0) {
                System.out.println("Erreur génération keystore");
                return false;
            }

            // 2. Exporter certificat public
            ProcessBuilder exportCert = new ProcessBuilder(
                    "keytool",
                    "-exportcert",
                    "-alias", alias,
                    "-keystore", keystorePath,
                    "-storetype", "PKCS12",
                    "-storepass", STORE_PASSWORD,
                    "-file", certPath
            );

            exportCert.inheritIO();
            int exportResult = exportCert.start().waitFor();

            if (exportResult != 0) {
                System.out.println("Erreur export certificat");
                return false;
            }

            // 3. Sauvegarder en base
            saveCertificateInfo(
                    user.getId(),
                    user.getEmail(),
                    user.getRole(),
                    alias,
                    keystorePath,
                    certPath
            );

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void saveCertificateInfo(int userId, String email, String role,
                                     String alias, String keystorePath,
                                     String certPath) throws Exception {

        String sql = """
                INSERT INTO user_certificates
                (user_id, email, role, alias_name, keystore_path, certificate_path, status, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, 'active', ?)
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
            ps.setString(7, expiresAt.toString());

            ps.executeUpdate();
        }
    }
}