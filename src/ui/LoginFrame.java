package ui;

import Client.AppSession;
import Client.ClientHashUtil;
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
    private JPanel card;

    public LoginFrame(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
    }

    private void initUI() {
        setTitle("ChriOnline");
        setSize(820, 640);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(13, 17, 27),
                        getWidth(), getHeight(), new Color(18, 26, 44)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBackground(UITheme.BG);

        card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(UITheme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(430, 590));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(36, 44, 36, 44));

        JLabel icon = new JLabel("🛍️");
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setFont(new Font("SansSerif", Font.PLAIN, 48));

        JPanel accent = new JPanel();
        accent.setOpaque(false);
        accent.setMaximumSize(new Dimension(40, 3));
        accent.setPreferredSize(new Dimension(40, 3));
        accent.setBackground(UITheme.GOLD);
        accent.setAlignmentX(Component.CENTER_ALIGNMENT);

        title = new JLabel(LanguageManager.getInstance().getText("login.title"));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(UITheme.TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));

        subtitle = new JLabel(LanguageManager.getInstance().getText("login.subtitle"));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(UITheme.MUTED);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        emailField = buildLabeledField(LanguageManager.getInstance().getText("login.email"));

        passwordPanel = UITheme.createPasswordFieldWithEye(
                LanguageManager.getInstance().getText("login.password")
        );
        passwordPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordPanel.setMaximumSize(new Dimension(342, 72));
        passwordPanel.setPreferredSize(new Dimension(342, 72));

        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setForeground(UITheme.RED);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        loginBtn = UITheme.primaryButton("  " + LanguageManager.getInstance().getText("login.button") + "  ");
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(342, 46));
        loginBtn.setPreferredSize(new Dimension(342, 46));

        registerBtn = UITheme.blueButton(LanguageManager.getInstance().getText("login.register"));
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerBtn.setMaximumSize(new Dimension(342, 42));
        registerBtn.setPreferredSize(new Dimension(342, 42));

        JButton rsaLoginBtn = UITheme.iconButton("🔑 Connexion Admin RSA", new Color(71, 85, 105));
        rsaLoginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        rsaLoginBtn.setMaximumSize(new Dimension(342, 38));
        rsaLoginBtn.setPreferredSize(new Dimension(342, 38));

        JButton languageBtn = buildLanguageButton();
        languageBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        languageBtn.setMaximumSize(new Dimension(160, 32));

        card.add(icon);
        card.add(Box.createVerticalStrut(10));
        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(28));
        card.add(emailField);
        card.add(Box.createVerticalStrut(12));
        card.add(passwordPanel);
        card.add(Box.createVerticalStrut(8));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(10));
        card.add(registerBtn);
        card.add(Box.createVerticalStrut(8));
        card.add(rsaLoginBtn);
        card.add(Box.createVerticalStrut(14));
        card.add(languageBtn);

        root.add(card);
        setContentPane(root);

        loginBtn.addActionListener(e -> doLogin());

        registerBtn.addActionListener(e -> {
            setVisible(false);
            new RegisterFrame(clientService, this).setVisible(true);
        });

        rsaLoginBtn.addActionListener(e -> {
            dispose();
            new AdminRSAuthFrame(clientService).setVisible(true);
        });

        emailField.addActionListener(e -> doLogin());
    }

    private JTextField buildLabeledField(String labelText) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(342, 72));
        wrapper.setPreferredSize(new Dimension(342, 72));
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel(labelText);
        lbl.setForeground(UITheme.MUTED);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setBorder(new EmptyBorder(0, 2, 4, 0));

        JTextField field = new JTextField();
        field.setBackground(UITheme.INPUT_BG);
        field.setForeground(UITheme.TEXT);
        field.setCaretColor(UITheme.SKY);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(9, 13, 9, 13)
        ));
        field.setSelectedTextColor(Color.WHITE);
        field.setSelectionColor(UITheme.BLUE);
        field.putClientProperty("label", labelText);

        return field;
    }

    private JButton buildLanguageButton() {
        JButton btn = new JButton(
                LanguageManager.getCurrentLanguage().getFlag() + " " +
                LanguageManager.getCurrentLanguage().getDisplayName()
        );
        btn.setBackground(new Color(30, 40, 58));
        btn.setForeground(UITheme.MUTED);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
            JPopupMenu langMenu = new JPopupMenu();
            langMenu.setBackground(UITheme.CARD);
            for (LanguageManager.Language lang : LanguageManager.Language.values()) {
                JMenuItem item = new JMenuItem(lang.getFlag() + "  " + lang.getDisplayName());
                item.setBackground(UITheme.CARD);
                item.setForeground(UITheme.TEXT);
                item.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                item.addActionListener(ev -> {
                    LanguageManager.setLanguage(lang);
                    btn.setText(lang.getFlag() + " " + lang.getDisplayName());
                    refreshUI();
                });
                langMenu.add(item);
            }
            langMenu.show(btn, 0, btn.getHeight());
        });

        return btn;
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

        // ⭐ Hachage SHA-256 côté client — le mot de passe en clair ne sort jamais
        final String hashedPassword;
        try {
            hashedPassword = ClientHashUtil.hashPasswordClient(password);
        } catch (Exception e) {
            statusLabel.setText("Erreur de sécurité. Réessayez.");
            return;
        }

        loginBtn.setEnabled(false);
        loginBtn.setText("Connexion...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    if (!clientService.connect()) {
                        String err = clientService.getLastConnectionError();
                        return "ERROR:TOO_MANY_CONNECTIONS".equals(err)
                                ? "ERROR:TOO_MANY_CONNECTIONS"
                                : "ERROR:SERVER_UNREACHABLE";
                    }
                    // ⭐ On envoie le hash SHA-256, jamais le mot de passe en clair
                    return clientService.login(email, hashedPassword);
                } catch (Exception e) {
                    return "ERROR:SERVER_UNREACHABLE";
                }
            }

            @Override
            protected void done() {
                loginBtn.setEnabled(true);
                loginBtn.setText("  " + LanguageManager.getInstance().getText("login.button") + "  ");
                try {
                    handleLoginResponse(get(), email);
                } catch (Exception ex) {
                    statusLabel.setText(LanguageManager.getInstance().getText("login.error.server"));
                }
            }
        }.execute();
    }

    private void handleLoginResponse(String response, String email) {
        if (response != null && response.startsWith("LOGIN_SUCCESS:")) {
            String[] parts = response.split(":", -1);
            if (parts.length < 3) {
                statusLabel.setText(LanguageManager.getInstance().getText("login.error.invalid"));
                return;
            }

            int userId = Integer.parseInt(parts[1]);
            String role = parts[2];

            if (parts.length >= 4) clientService.setSessionToken(parts[3]);

            AppSession session = new AppSession();
            session.setUserId(userId);
            session.setRole(role);

            String profileResponse = clientService.getProfile(userId);
            if (profileResponse != null && profileResponse.startsWith("PROFILE_DATA:")) {
                String[] fields = profileResponse.substring("PROFILE_DATA:".length()).split(";", -1);
                if (fields.length >= 1) session.setFullName(fields[0].trim());
            }

            dispose();
            if ("admin".equalsIgnoreCase(role)) {
                new ui.admin.AdminMainFrame(clientService, session).setVisible(true);
            } else {
                new ShopFrame(clientService, session).setVisible(true);
            }

        } else if ("ERROR:TOO_MANY_CONNECTIONS".equals(response)) {
            statusLabel.setText("Trop de connexions ouvertes.");
            JOptionPane.showMessageDialog(this,
                    "Trop de connexions ouvertes.\nFermez une fenêtre puis réessayez.",
                    "Connexion refusée", JOptionPane.WARNING_MESSAGE);

        } else if ("ERROR:TOO_MANY_ATTEMPTS".equals(response)) {
            statusLabel.setText("Trop de tentatives. Réessayez dans 5 min.");
            JOptionPane.showMessageDialog(this,
                    "Trop de tentatives.\nAccès bloqué temporairement (5 min).",
                    "Connexion bloquée", JOptionPane.WARNING_MESSAGE);

        } else if ("ERROR:ACCOUNT_NOT_ACTIVE".equals(response)) {
            JOptionPane.showMessageDialog(this,
                    "Compte non activé.\nVérifiez votre code OTP.",
                    "Compte inactif", JOptionPane.WARNING_MESSAGE);
            setVisible(false);
            new OtpFrame(clientService, email, this).setVisible(true);

        } else if (response != null && (
                response.startsWith("ERROR:SERVER") ||
                response.startsWith("ERROR:NO_RESPONSE") ||
                response.startsWith("ERROR:COMMUNICATION"))) {
            statusLabel.setText(LanguageManager.getInstance().getText("login.error.server"));

        } else {
            statusLabel.setText(LanguageManager.getInstance().getText("login.error.invalid"));
        }
    }

    private void refreshUI() {
        setTitle(LanguageManager.getInstance().getText("login.title"));
        title.setText(LanguageManager.getInstance().getText("login.title"));
        subtitle.setText(LanguageManager.getInstance().getText("login.subtitle"));

        emailField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(9, 13, 9, 13)
        ));

        loginBtn.setText("  " + LanguageManager.getInstance().getText("login.button") + "  ");
        registerBtn.setText(LanguageManager.getInstance().getText("login.register"));

        JPanel newPasswordPanel = UITheme.createPasswordFieldWithEye(
                LanguageManager.getInstance().getText("login.password")
        );
        newPasswordPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        newPasswordPanel.setMaximumSize(new Dimension(342, 72));
        newPasswordPanel.setPreferredSize(new Dimension(342, 72));

        int index = -1;
        for (int i = 0; i < card.getComponentCount(); i++) {
            if (card.getComponent(i) == passwordPanel) { index = i; break; }
        }
        if (index != -1) {
            card.remove(index);
            passwordPanel = newPasswordPanel;
            card.add(passwordPanel, index);
        }

        statusLabel.setText(" ");
        card.revalidate();
        card.repaint();
    }
}