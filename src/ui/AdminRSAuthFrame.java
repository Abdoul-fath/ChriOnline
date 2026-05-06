package ui;

import Client.AppSession;
import Client.ClientSocketService;
import security.RSAAuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminRSAuthFrame extends JFrame {

    private static final String DEFAULT_KEY_ALIAS = "user_2";

    private static final String KEYSTORE_PATH =
            "C:/Users/abdoo/eclipse-workspace/tp1/keys/users/hisouabdoulfatah_at_gmail_com_keystore.p12";

    private final ClientSocketService clientService;

    private JTextField emailField;
    private JPasswordField keystorePasswordField;
    private JButton authenticateButton;
    private JLabel statusLabel;

    public AdminRSAuthFrame(ClientSocketService clientService) {
        this.clientService = clientService;
        initComponents();

        setTitle("Authentification Admin - RSA");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(650, 500);
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
        card.setPreferredSize(new Dimension(540, 430));

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

        emailField = UITheme.textField();
        emailField.setMaximumSize(new Dimension(430, 48));
        emailField.setBorder(UITheme.titledBorder("Email administrateur"));
        emailField.setAlignmentX(Component.CENTER_ALIGNMENT);

        keystorePasswordField = new JPasswordField();
        keystorePasswordField.setMaximumSize(new Dimension(430, 48));
        keystorePasswordField.setBackground(UITheme.INPUT_BG);
        keystorePasswordField.setForeground(UITheme.TEXT);
        keystorePasswordField.setCaretColor(UITheme.TEXT);
        keystorePasswordField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        keystorePasswordField.setBorder(UITheme.titledBorder("Mot de passe du certificat"));
        keystorePasswordField.setEchoChar('●');
        keystorePasswordField.setAlignmentX(Component.CENTER_ALIGNMENT);

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
        card.add(Box.createVerticalStrut(16));
        card.add(keystorePasswordField);
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
        String keystorePassword = new String(keystorePasswordField.getPassword()).trim();

        if (email.isEmpty() || keystorePassword.isEmpty()) {
            statusLabel.setForeground(UITheme.RED);
            statusLabel.setText("✗ Email et mot de passe requis");
            return;
        }

        authenticateButton.setEnabled(false);
        statusLabel.setForeground(UITheme.MUTED);
        statusLabel.setText("Authentification en cours...");

        new SwingWorker<Boolean, Void>() {

            private AppSession session;

            @Override
            protected Boolean doInBackground() {
                try {
                    if (!clientService.connect()) {
                        return false;
                    }

                    String response = clientService.requestAdminChallenge(email);

                    if (response == null || !response.startsWith("CHALLENGE:")) {
                        System.out.println("Réponse challenge : " + response);
                        return false;
                    }

                    String challenge = response.substring("CHALLENGE:".length());

                    RSAAuthService rsaService = new RSAAuthService(
                            KEYSTORE_PATH,
                            keystorePassword,
                            DEFAULT_KEY_ALIAS
                    );

                    String signature = rsaService.signerChallenge(challenge);

                    String verifyResponse = clientService.verifyAdminSignature(
                            email,
                            signature,
                            challenge
                    );

                    if (verifyResponse == null || !verifyResponse.equals("ADMIN_AUTH_SUCCESS")) {
                        System.out.println("Réponse vérification : " + verifyResponse);
                        return false;
                    }

                    session = new AppSession();
                    session.setRole("admin");

                    String profileResponse = clientService.getProfileByEmail(email);

                    if (profileResponse != null && profileResponse.startsWith("PROFILE_DATA:")) {
                        String data = profileResponse.substring("PROFILE_DATA:".length());
                        String[] fields = data.split(";", -1);

                        if (fields.length >= 1) {
                            session.setFullName(fields[0].trim());
                        }

                        if (fields.length >= 7) {
                            try {
                                session.setUserId(Integer.parseInt(fields[6]));
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    return true;

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