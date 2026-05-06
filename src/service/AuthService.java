package service;

import dao.UserCertificateDAO;
import dao.UserDAO;
import model.Client;
import model.User;
import model.UserCertificateInfo;
import security.RSAAuthService;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {

    private final UserDAO userDAO;
    private final UserCertificateDAO certificateDAO;
    private final UserCertificateService certificateService;

    private static final Map<String, String> adminChallenges = new ConcurrentHashMap<>();

    private static final String TRUSTSTORE_PATH =
            "C:/Users/abdoo/eclipse-workspace/tp1/keys/server-truststore.p12";

    private static final String TRUSTSTORE_PASSWORD = "hisboula"; // mot de passe du truststore

    private static final String CA_ALIAS = "chrionline-ca";

    public AuthService() {
        this.userDAO = new UserDAO();
        this.certificateDAO = new UserCertificateDAO();
        this.certificateService = new UserCertificateService();
    }

    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Erreur hash : " + e.getMessage());
            return null;
        }
    }

    public boolean checkPassword(String password, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) return false;

        String[] parts = storedHash.split(":");
        if (parts.length != 2) return false;

        String salt = parts[0];
        String hash = parts[1];

        return hash.equals(hashPassword(password, salt));
    }

    public User login(String email, String password) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return null;
        }

        User user = userDAO.findByEmail(email);

        if (user == null) return null;

        if (!"active".equalsIgnoreCase(user.getStatus())) {
            System.out.println("Compte non actif : " + email);
            return null;
        }

        if (checkPassword(password, user.getPassword())) {
            System.out.println("Connexion réussie : " + user.getEmail());
            return user;
        }

        return null;
    }

    public boolean registerPending(String nom, String prenom, String email,
                                   String password, String address,
                                   String phone, String ville) {

        if (nom == null || nom.isBlank()
                || prenom == null || prenom.isBlank()
                || email == null || email.isBlank()
                || password == null || password.isBlank()) {
            return false;
        }

        if (userDAO.emailExists(email)) {
            return false;
        }

        String salt = generateSalt();
        String hashedPassword = salt + ":" + hashPassword(password, salt);

        Client client = new Client(nom, prenom, email, hashedPassword, address, phone, ville);
        return userDAO.savePendingClient(client);
    }

    public boolean emailExists(String email) {
        return userDAO.emailExists(email);
    }

    public boolean isAccountActive(String email) {
        return userDAO.isAccountActive(email);
    }

    public User getUserByUsername(String email) {
        return userDAO.findByEmail(email);
    }

    // =========================================================
    // RSA ADMIN AUTHENTICATION
    // =========================================================

    public String generateAdminChallenge(String email) {
        try {
            User user = userDAO.findByEmail(email);

            if (user == null) {
                System.out.println("Utilisateur introuvable : " + email);
                return null;
            }

            if (!"admin".equalsIgnoreCase(user.getRole())) {
                System.out.println("Utilisateur non admin : " + email);
                return null;
            }

            if (!"active".equalsIgnoreCase(user.getStatus())) {
                System.out.println("Compte admin non actif : " + email);
                return null;
            }

            UserCertificateInfo cert = getOrCreateValidCertificate(user);

            if (cert == null) {
                System.out.println("Impossible d'obtenir un certificat valide pour : " + email);
                return null;
            }

            byte[] bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);

            String challenge = Base64.getEncoder().encodeToString(bytes);
            adminChallenges.put(email, challenge);

            return challenge;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean verifyAdminSignature(String email, String signatureBase64, String challenge) {
        try {
            User user = userDAO.findByEmail(email);

            if (user == null) return false;
            if (!"admin".equalsIgnoreCase(user.getRole())) return false;
            if (!"active".equalsIgnoreCase(user.getStatus())) return false;

            String savedChallenge = adminChallenges.get(email);

            if (savedChallenge == null) {
                System.out.println("Aucun challenge sauvegardé pour : " + email);
                return false;
            }

            if (!savedChallenge.equals(challenge)) {
                System.out.println("Challenge incorrect pour : " + email);
                return false;
            }

            UserCertificateInfo cert = getOrCreateValidCertificate(user);

            if (cert == null) {
                System.out.println("Aucun certificat valide pour : " + email);
                return false;
            }

            boolean valid = RSAAuthService.verifierAvecTruststore(
                    challenge,
                    signatureBase64,
                    cert.getCertificatePath(),
                    TRUSTSTORE_PATH,
                    TRUSTSTORE_PASSWORD,
                    CA_ALIAS
            );

            if (valid) {
                adminChallenges.remove(email);
                System.out.println("Authentification RSA admin réussie avec truststore : " + email);
            } else {
                System.out.println("Signature RSA invalide pour : " + email);
            }

            return valid;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private UserCertificateInfo getOrCreateValidCertificate(User user) {
        try {
            String email = user.getEmail();

            UserCertificateInfo cert = certificateDAO.findActiveByEmail(email);

            boolean mustCreate = false;

            if (cert == null) {
                System.out.println("Aucun certificat trouvé pour : " + email);
                mustCreate = true;
            } else {
                File keystoreFile = new File(cert.getKeystorePath());
                File certificateFile = new File(cert.getCertificatePath());

                if (!keystoreFile.exists()) {
                    System.out.println("Keystore absent : " + cert.getKeystorePath());
                    mustCreate = true;
                }

                if (!certificateFile.exists()) {
                    System.out.println("Certificat absent : " + cert.getCertificatePath());
                    mustCreate = true;
                }

                if (!cert.isActiveAndValid()) {
                    System.out.println("Certificat expiré ou révoqué : " + email);
                    mustCreate = true;
                }
            }

            if (mustCreate) {
                System.out.println("Génération automatique du certificat pour : " + email);

                boolean created = certificateService.createCertificateForUser(user.getId());

                if (!created) {
                    System.out.println("Erreur génération certificat pour : " + email);
                    return null;
                }

                cert = certificateDAO.findActiveByEmail(email);
            }

            if (cert == null) {
                return null;
            }

            File keystoreFile = new File(cert.getKeystorePath());
            File certificateFile = new File(cert.getCertificatePath());

            if (!keystoreFile.exists() || !certificateFile.exists()) {
                System.out.println("Fichiers certificat/keystore absents.");
                return null;
            }

            if (!cert.isActiveAndValid()) {
                System.out.println("Certificat non valide.");
                return null;
            }

            return cert;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}