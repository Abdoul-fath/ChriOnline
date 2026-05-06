package ui;

import Client.AppSession;
import Client.ClientSocketService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final ClientSocketService clientService;

    private JTextField emailField;
    private JPanel passwordPanel;
    private JLabel statusLabel;
    private JLabel title;
    private JLabel subtitle;
    private JButton loginBtn;
    private JButton registerBtn;
    private JButton languageBtn;
    private JPanel card;

    public LoginFrame(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
    }

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("login.title"));
        setSize(800, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new GridBagLayout());

        card = UITheme.cardPanel();
        card.setPreferredSize(new Dimension(480, 620));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(32, 42, 32, 42)
        ));

        JLabel icon = new JLabel("🛍️");
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setFont(new Font("SansSerif", Font.PLAIN, 58));
        icon.setForeground(UITheme.GOLD);

        title = new JLabel(LanguageManager.getInstance().getText("login.title"));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 32));

        subtitle = new JLabel(LanguageManager.getInstance().getText("login.subtitle"));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(UITheme.MUTED);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));

        emailField = createStyledTextField(LanguageManager.getInstance().getText("login.email"));

        passwordPanel = UITheme.createPasswordFieldWithEye(
                LanguageManager.getInstance().getText("login.password")
        );
        passwordPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordPanel.setMaximumSize(new Dimension(340, 60));
        passwordPanel.setPreferredSize(new Dimension(340, 60));

        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setForeground(UITheme.RED);
        statusLabel.setFont(UITheme.smallFont());

        loginBtn = UITheme.primaryButton("🔐 " + LanguageManager.getInstance().getText("login.button"));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(340, 48));
        loginBtn.setPreferredSize(new Dimension(340, 48));

        registerBtn = UITheme.blueButton("📝 " + LanguageManager.getInstance().getText("login.register"));
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerBtn.setMaximumSize(new Dimension(340, 48));
        registerBtn.setPreferredSize(new Dimension(340, 48));

        JButton rsaLoginBtn = UITheme.blueButton("🔑 Connexion Admin avec clé RSA");
        rsaLoginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        rsaLoginBtn.setMaximumSize(new Dimension(340, 48));
        rsaLoginBtn.setPreferredSize(new Dimension(340, 48));
        rsaLoginBtn.setBackground(new Color(88, 199, 250));
        rsaLoginBtn.setForeground(Color.WHITE);

        languageBtn = new JButton(
                LanguageManager.getCurrentLanguage().getFlag() + " " +
                        LanguageManager.getCurrentLanguage().getDisplayName()
        );
        languageBtn.setBackground(UITheme.CARD);
        languageBtn.setForeground(Color.WHITE);
        languageBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        languageBtn.setFocusPainted(false);
        languageBtn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        languageBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        languageBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        languageBtn.setMaximumSize(new Dimension(170, 35));

        languageBtn.addActionListener(e -> {
            JPopupMenu langMenu = new JPopupMenu();

            for (LanguageManager.Language lang : LanguageManager.Language.values()) {
                JMenuItem item = new JMenuItem(lang.getFlag() + " " + lang.getDisplayName());
                item.addActionListener(ev -> {
                    LanguageManager.setLanguage(lang);
                    refreshUI();
                });
                langMenu.add(item);
            }

            langMenu.show(languageBtn, 0, languageBtn.getHeight());
        });

        rsaLoginBtn.addActionListener(e -> {
            dispose();
            new AdminRSAuthFrame(clientService).setVisible(true);
        });

        card.add(icon);
        card.add(Box.createVerticalStrut(10));
        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(28));
        card.add(emailField);
        card.add(Box.createVerticalStrut(14));
        card.add(passwordPanel);
        card.add(Box.createVerticalStrut(10));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(18));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(12));
        card.add(registerBtn);
        card.add(Box.createVerticalStrut(12));
        card.add(rsaLoginBtn);
        card.add(Box.createVerticalStrut(12));
        card.add(languageBtn);

        root.add(card);
        setContentPane(root);

        loginBtn.addActionListener(e -> doLogin());

        registerBtn.addActionListener(e -> {
            setVisible(false);
            new RegisterFrame(clientService, this).setVisible(true);
        });
    }

    private JTextField createStyledTextField(String titleText) {
        JTextField field = UITheme.textField();
        field.setMaximumSize(new Dimension(340, 52));
        field.setPreferredSize(new Dimension(340, 52));
        field.setBorder(UITheme.titledBorder(titleText));
        return field;
    }

    private void doLogin() {
        String email = emailField.getText().trim();

        JPasswordField passwordField = UITheme.getPasswordFieldFromPanel(passwordPanel);
        String password = passwordField == null ? "" : new String(passwordField.getPassword()).trim();

        statusLabel.setForeground(UITheme.RED);
        statusLabel.setText(" ");

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText(LanguageManager.getInstance().getText("login.error.empty"));
            return;
        }

        loginBtn.setEnabled(false);

        try {
            if (!clientService.connect()) {
                if ("ERROR:TOO_MANY_CONNECTIONS".equals(clientService.getLastConnectionError())) {
                    statusLabel.setText("Trop de connexions ouvertes. Fermez une fenêtre puis réessayez.");

                    JOptionPane.showMessageDialog(
                            this,
                            "Trop de connexions sont ouvertes depuis votre ordinateur.\nFermez une autre fenêtre de l'application puis réessayez.",
                            "Connexion refusée",
                            JOptionPane.WARNING_MESSAGE
                    );
                } else {
                    statusLabel.setText(LanguageManager.getInstance().getText("login.error.server"));
                }
                return;
            }

            String response = clientService.login(email, password);

            if (response != null && response.startsWith("LOGIN_SUCCESS:")) {
                String[] parts = response.split(":", -1);

                if (parts.length < 3) {
                    statusLabel.setText(LanguageManager.getInstance().getText("login.error.invalid"));
                    return;
                }

                int userId = Integer.parseInt(parts[1]);
                String role = parts[2];

                if (parts.length >= 4) {
                    clientService.setSessionToken(parts[3]);
                }

                AppSession session = new AppSession();
                session.setUserId(userId);
                session.setRole(role);

                String profileResponse = clientService.getProfile(userId);
                if (profileResponse != null && profileResponse.startsWith("PROFILE_DATA:")) {
                    String data = profileResponse.substring("PROFILE_DATA:".length());
                    String[] fields = data.split(";", -1);

                    if (fields.length >= 1) {
                        session.setFullName(fields[0].trim());
                    }
                }

                dispose();

                if ("admin".equalsIgnoreCase(role)) {
                    new ui.admin.AdminMainFrame(clientService, session).setVisible(true);
                } else {
                    new ShopFrame(clientService, session).setVisible(true);
                }

            } else if ("ERROR:TOO_MANY_CONNECTIONS".equals(response)) {
                statusLabel.setText("Trop de connexions ouvertes. Fermez une fenêtre puis réessayez.");

                JOptionPane.showMessageDialog(
                        this,
                        "Trop de connexions sont ouvertes depuis votre ordinateur.\nFermez une autre fenêtre de l'application puis réessayez.",
                        "Connexion refusée",
                        JOptionPane.WARNING_MESSAGE
                );

            } else if ("ERROR:TOO_MANY_ATTEMPTS".equals(response)) {
                statusLabel.setForeground(UITheme.RED);
                statusLabel.setText("Trop de tentatives. Réessayez après 5 minutes.");

                JOptionPane.showMessageDialog(
                        this,
                        "Trop de tentatives de connexion.\nVotre accès est bloqué temporairement.\nRéessayez après 5 minutes.",
                        "Connexion bloquée",
                        JOptionPane.WARNING_MESSAGE
                );

            } else if ("ERROR:ACCOUNT_NOT_ACTIVE".equals(response)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Votre compte n'est pas encore activé.\nVeuillez vérifier le code OTP.",
                        "Compte non activé",
                        JOptionPane.WARNING_MESSAGE
                );

                setVisible(false);
                new OtpFrame(clientService, email, this).setVisible(true);

            } else if ("ERROR:SERVER_UNREACHABLE".equals(response)
                    || "ERROR:NO_RESPONSE".equals(response)
                    || "ERROR:COMMUNICATION".equals(response)) {
                statusLabel.setText(LanguageManager.getInstance().getText("login.error.server"));

            } else {
                statusLabel.setText(LanguageManager.getInstance().getText("login.error.invalid"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText(LanguageManager.getInstance().getText("login.error.server"));
        } finally {
            loginBtn.setEnabled(true);
        }
    }

    private void refreshUI() {
        setTitle(LanguageManager.getInstance().getText("login.title"));
        title.setText(LanguageManager.getInstance().getText("login.title"));
        subtitle.setText(LanguageManager.getInstance().getText("login.subtitle"));

        languageBtn.setText(
                LanguageManager.getCurrentLanguage().getFlag() + " " +
                        LanguageManager.getCurrentLanguage().getDisplayName()
        );

        emailField.setBorder(UITheme.titledBorder(
                LanguageManager.getInstance().getText("login.email")
        ));

        JPanel newPasswordPanel = UITheme.createPasswordFieldWithEye(
                LanguageManager.getInstance().getText("login.password")
        );
        newPasswordPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        newPasswordPanel.setMaximumSize(new Dimension(340, 60));
        newPasswordPanel.setPreferredSize(new Dimension(340, 60));

        int index = -1;
        for (int i = 0; i < card.getComponentCount(); i++) {
            if (card.getComponent(i) == passwordPanel) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            card.remove(index);
            passwordPanel = newPasswordPanel;
            card.add(passwordPanel, index);
        }

        loginBtn.setText("🔐 " + LanguageManager.getInstance().getText("login.button"));
        registerBtn.setText("📝 " + LanguageManager.getInstance().getText("login.register"));

        statusLabel.setForeground(UITheme.RED);
        statusLabel.setText(" ");

        card.revalidate();
        card.repaint();
    }
}