package ui;

import Client.AppSession;
import Client.ClientSocketService;
import security.RSAAuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminRSAuthFrame extends JFrame {

    private static final String BASE_KEYS_DIR =
            "C:/Users/abdoo/eclipse-workspace/tp1/keys/users/";

    private final ClientSocketService clientService;

    private JTextField emailField;
    private JButton authenticateButton;
    private JLabel statusLabel;

    public AdminRSAuthFrame(ClientSocketService clientService) {
        this.clientService = clientService;
        initComponents();
        setTitle("Authentification Admin - RSA");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(650, 440);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void initComponents() {
        JPanel root = UITheme.darkPanel();
        root.setLayout(new GridBagLayout());

        JPanel card = UITheme.cardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(28, 38, 28, 38)
        ));
        card.setPreferredSize(new Dimension(540, 380));

        JLabel icon = new JLabel("🔐");
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setFont(new Font("SansSerif", Font.PLAIN, 52));
        icon.setForeground(UITheme.GOLD);

        JLabel title = new JLabel("Authentification Admin RSA");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        JLabel subtitle = new JLabel("Connexion sécurisée par certificat administrateur");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(UITheme.MUTED);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JLabel infoLabel = new JLabel("🔒 Mot de passe du certificat géré automatiquement");
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoLabel.setForeground(UITheme.MUTED);
        infoLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));

        emailField = UITheme.textField();
        emailField.setMaximumSize(new Dimension(430, 48));
        emailField.setBorder(UITheme.titledBorder("Email administrateur"));
        emailField.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setForeground(UITheme.MUTED);
        statusLabel.setFont(UITheme.smallFont());

        authenticateButton = UITheme.primaryButton("✅ S'authentifier avec RSA");
        authenticateButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        authenticateButton.setMaximumSize(new Dimension(430, 48));

        JButton backButton = UITheme.blueButton("← Retour");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setMaximumSize(new Dimension(430, 45));

        card.add(icon);
        card.add(Box.createVerticalStrut(8));
        card.add(title);
        card.add(Box.createVerticalStrut(5));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(26));
        card.add(emailField);
        card.add(Box.createVerticalStrut(8));
        card.add(infoLabel);
        card.add(Box.createVerticalStrut(14));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(authenticateButton);
        card.add(Box.createVerticalStrut(10));
        card.add(backButton);

        root.add(card);
        setContentPane(root);

        authenticateButton.addActionListener(e -> authenticate());
        backButton.addActionListener(e -> {
            dispose();
            new LoginFrame(clientService).setVisible(true);
        });
    }

    private void authenticate() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            statusLabel.setForeground(UITheme.RED);
            statusLabel.setText("✗ Email requis");
            return;
        }

        String safeEmail    = email.replace("@", "_at_").replace(".", "_");
        String keystorePath = BASE_KEYS_DIR + safeEmail + "_keystore.p12";

        authenticateButton.setEnabled(false);
        statusLabel.setForeground(UITheme.MUTED);
        statusLabel.setText("Authentification en cours...");

        final String finalKeystorePath = keystorePath;

        new SwingWorker<Boolean, Void>() {

            private AppSession session;

            @Override
            protected Boolean doInBackground() {
                try {
                    if (!clientService.connect()) return false;

                    // ── Récupérer profil → userId → alias ────────────────
                    String profileResponse = clientService.getProfileByEmail(email);
                    if (profileResponse == null ||
                            !profileResponse.startsWith("PROFILE_DATA:")) {
                        System.out.println("Profil introuvable : " + profileResponse);
                        return false;
                    }

                    String[] fields = profileResponse
                            .substring("PROFILE_DATA:".length())
                            .split(";", -1);

                    if (fields.length < 7) {
                        System.out.println("Profil incomplet");
                        return false;
                    }

                    int userId   = Integer.parseInt(fields[6].trim());
                    String alias = "user_" + userId;

                    System.out.println("Alias utilisé : " + alias);
                    System.out.println("Keystore : " + finalKeystorePath);

                    // ── Demande du challenge ──────────────────────────────
                    String response = clientService.requestAdminChallenge(email);
                    if (response == null || !response.startsWith("CHALLENGE:")) {
                        System.out.println("Réponse challenge : " + response);
                        return false;
                    }

                    String challenge = response.substring("CHALLENGE:".length());

                    // ── Récupérer le mot de passe déchiffré ───────────────
                    String keystorePassword = clientService.getKeystorePassword(email);
                    if (keystorePassword == null) {
                        System.out.println("Mot de passe keystore introuvable");
                        return false;
                    }

                    // ── Signer le challenge avec la clé privée ────────────
                    RSAAuthService rsaService = new RSAAuthService(
                            finalKeystorePath,
                            keystorePassword,
                            alias
                    );

                    String signature = rsaService.signerChallenge(challenge);

                    String verifyResponse = clientService.verifyAdminSignature(
                            email, signature, challenge);

                    System.out.println("Réponse vérification : " + verifyResponse);

                    // ⭐ CORRECTION : startsWith car le serveur retourne
                    // "ADMIN_AUTH_SUCCESS:userId:role:token"
                    if (verifyResponse != null &&
                            verifyResponse.startsWith("ADMIN_AUTH_SUCCESS:")) {

                        String[] parts = verifyResponse.split(":", -1);
                        // Format : ADMIN_AUTH_SUCCESS:userId:role:token
                        if (parts.length >= 4) {
                            // ⭐ Stocker le token de session
                            clientService.setSessionToken(parts[3]);
                        }

                        session = new AppSession();
                        session.setRole("admin");
                        session.setFullName(fields[0].trim());
                        session.setUserId(userId);

                        return true;
                    }

                    System.out.println("Échec vérification : " + verifyResponse);
                    return false;

                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                authenticateButton.setEnabled(true);
                try {
                    if (get()) {
                        statusLabel.setForeground(UITheme.GREEN);
                        statusLabel.setText("✓ Authentification réussie");
                        dispose();
                        new ui.admin.AdminMainFrame(clientService, session).setVisible(true);
                    } else {
                        statusLabel.setForeground(UITheme.RED);
                        statusLabel.setText("✗ Échec authentification RSA");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    statusLabel.setForeground(UITheme.RED);
                    statusLabel.setText("✗ Erreur authentification");
                }
            }
        }.execute();
    }
}