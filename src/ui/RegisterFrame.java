package ui;

import Client.ClientHashUtil;
import Client.ClientSocketService;
import utils.PasswordValidator;
import utils.PasswordValidator.ValidationResult;
import utils.PasswordValidator.PasswordStrength;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

public class RegisterFrame extends JFrame {

    private final ClientSocketService clientService;
    private final JFrame backFrame;

    private JTextField nomField;
    private JTextField prenomField;
    private JTextField emailField;
    private JPanel passwordPanel;
    private JPanel confirmPasswordPanel;
    private JTextField addressField;
    private JTextField phoneField;
    private JTextField villeField;
    private JLabel statusLabel;

    // ⭐ Ajout : indicateur de force + requirements
    private JLabel strengthLabel;
    private JPanel requirementsPanel;

    public RegisterFrame(ClientSocketService clientService, JFrame backFrame) {
        this.clientService = clientService;
        this.backFrame = backFrame;
        initUI();
    }

    private void initUI() {
        setTitle("ChriOnline — Inscription");
        setSize(820, 880);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(13, 17, 27),
                        getWidth(), getHeight(), new Color(18, 26, 44)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBackground(UITheme.BG);

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
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
        card.setPreferredSize(new Dimension(460, 1000)); // ⭐ plus grand pour les nouveaux éléments
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(32, 44, 32, 44));

        // Header
        JLabel icon = new JLabel("📝");
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setFont(new Font("SansSerif", Font.PLAIN, 44));

        JLabel title = new JLabel("Créer un compte");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(UITheme.TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));

        JLabel subtitle = new JLabel("Rejoignez ChriOnline");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(UITheme.MUTED);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Champs
        nomField    = buildField("Nom");
        prenomField = buildField("Prénom");
        emailField  = buildField("Email");

        passwordPanel = UITheme.createPasswordFieldWithEye("Mot de passe");
        passwordPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordPanel.setMaximumSize(new Dimension(372, 72));
        passwordPanel.setPreferredSize(new Dimension(372, 72));

        // ⭐ Indicateur de force du mot de passe
        strengthLabel = new JLabel(" ");
        strengthLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        strengthLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // ⭐ Panel des exigences
        requirementsPanel = createRequirementsPanel();

        confirmPasswordPanel = UITheme.createPasswordFieldWithEye("Confirmer le mot de passe");
        confirmPasswordPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmPasswordPanel.setMaximumSize(new Dimension(372, 72));
        confirmPasswordPanel.setPreferredSize(new Dimension(372, 72));

        addressField = buildField("Adresse");
        phoneField   = buildField("Téléphone");
        villeField   = buildField("Ville");

        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setForeground(UITheme.RED);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton registerBtn = UITheme.primaryButton("S'inscrire");
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerBtn.setMaximumSize(new Dimension(372, 46));
        registerBtn.setPreferredSize(new Dimension(372, 46));

        JButton backBtn = UITheme.blueButton("← Retour");
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backBtn.setMaximumSize(new Dimension(372, 40));
        backBtn.setPreferredSize(new Dimension(372, 40));

        // Assemblage — ton ordre + nouveaux éléments dans la section Sécurité
        card.add(icon);
        card.add(Box.createVerticalStrut(8));
        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(26));

        card.add(sectionLabel("Identité"));
        card.add(Box.createVerticalStrut(8));
        card.add(nomField);
        card.add(Box.createVerticalStrut(10));
        card.add(prenomField);
        card.add(Box.createVerticalStrut(10));
        card.add(emailField);
        card.add(Box.createVerticalStrut(18));

        card.add(sectionLabel("Sécurité"));
        card.add(Box.createVerticalStrut(8));
        card.add(passwordPanel);
        card.add(Box.createVerticalStrut(6));
        card.add(strengthLabel);        // ⭐
        card.add(Box.createVerticalStrut(6));
        card.add(requirementsPanel);    // ⭐
        card.add(Box.createVerticalStrut(10));
        card.add(confirmPasswordPanel);
        card.add(Box.createVerticalStrut(18));

        card.add(sectionLabel("Coordonnées"));
        card.add(Box.createVerticalStrut(8));
        card.add(addressField);
        card.add(Box.createVerticalStrut(10));
        card.add(phoneField);
        card.add(Box.createVerticalStrut(10));
        card.add(villeField);
        card.add(Box.createVerticalStrut(14));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(registerBtn);
        card.add(Box.createVerticalStrut(10));
        card.add(backBtn);

        // ⭐ ScrollPane pour que tout soit accessible
        JScrollPane scrollPane = new JScrollPane(card);
        scrollPane.setBorder(null);
        scrollPane.setBackground(UITheme.BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        root.add(scrollPane, gbc);
        setContentPane(root);

        // ⭐ Listener temps réel sur le mot de passe
        JPasswordField passwordField = UITheme.getPasswordFieldFromPanel(passwordPanel);
        if (passwordField != null) {
            passwordField.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e)  { updatePasswordStrength(); }
                @Override public void removeUpdate(DocumentEvent e)  { updatePasswordStrength(); }
                @Override public void changedUpdate(DocumentEvent e) { updatePasswordStrength(); }
            });
        }

        registerBtn.addActionListener(e -> register());
        backBtn.addActionListener(e -> { backFrame.setVisible(true); dispose(); });
    }

    // ──────────────────────────────────────────────────────────────
    // HELPERS UI — ton style conservé
    // ──────────────────────────────────────────────────────────────

    private JTextField buildField(String labelText) {
        JTextField field = new JTextField();
        field.setBackground(UITheme.INPUT_BG);
        field.setForeground(UITheme.TEXT);
        field.setCaretColor(UITheme.SKY);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setSelectedTextColor(Color.WHITE);
        field.setSelectionColor(UITheme.BLUE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(9, 13, 9, 13)
        ));
        field.setMaximumSize(new Dimension(372, 46));
        field.setPreferredSize(new Dimension(372, 46));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.putClientProperty("JTextField.placeholderText", labelText);
        return field;
    }

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setForeground(UITheme.MUTED);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setBorder(new EmptyBorder(0, 2, 0, 0));
        return lbl;
    }

    // ──────────────────────────────────────────────────────────────
    // ⭐ PASSWORD STRENGTH — repris du code ami
    // ──────────────────────────────────────────────────────────────

    private JPanel createRequirementsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.CARD);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setMaximumSize(new Dimension(372, 140));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel header = new JLabel("Exigences du mot de passe :");
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        panel.add(header);
        panel.add(Box.createVerticalStrut(6));

        List<String> requirements = PasswordValidator.getRequirementsList();
        for (String req : requirements) {
            JLabel label = new JLabel("❌ " + req);
            label.setForeground(UITheme.MUTED);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            label.setName(req);
            panel.add(label);
            panel.add(Box.createVerticalStrut(3));
        }

        return panel;
    }

    private void updatePasswordStrength() {
        JPasswordField passwordField = UITheme.getPasswordFieldFromPanel(passwordPanel);
        if (passwordField == null) return;
        String password = new String(passwordField.getPassword());

        ValidationResult result = PasswordValidator.validate(password);
        PasswordStrength strength = result.getStrength();

        strengthLabel.setText(strength.getIcon() + " " + strength.getLabel());
        switch (strength) {
            case WEAK      -> strengthLabel.setForeground(UITheme.RED);
            case MEDIUM    -> strengthLabel.setForeground(Color.ORANGE);
            case STRONG    -> strengthLabel.setForeground(Color.YELLOW);
            case VERY_STRONG -> strengthLabel.setForeground(Color.GREEN);
        }

        updateRequirementsChecks(password);
    }

    private void updateRequirementsChecks(String password) {
        for (Component comp : requirementsPanel.getComponents()) {
            if (comp instanceof JLabel label) {
                String name = label.getName();
                if (name == null) continue;

                boolean ok = false;
                if      (name.contains("8 caractères")) ok = PasswordValidator.hasMinLength(password);
                else if (name.contains("majuscule"))    ok = PasswordValidator.hasUpperCase(password);
                else if (name.contains("minuscule"))    ok = PasswordValidator.hasLowerCase(password);
                else if (name.contains("chiffre"))      ok = PasswordValidator.hasDigit(password);
                else if (name.contains("spécial"))      ok = PasswordValidator.hasSpecialChar(password);

                label.setText((ok ? "✅" : "❌") + " " + name);
                label.setForeground(ok ? Color.GREEN : UITheme.MUTED);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // REGISTER — ton flux + hachage SHA-256 ajouté
    // ──────────────────────────────────────────────────────────────

    private void register() {
        String nom     = nomField.getText().trim();
        String prenom  = prenomField.getText().trim();
        String email   = emailField.getText().trim();
        String address = addressField.getText().trim();
        String phone   = phoneField.getText().trim();
        String ville   = villeField.getText().trim();

        JPasswordField pf  = UITheme.getPasswordFieldFromPanel(passwordPanel);
        JPasswordField cpf = UITheme.getPasswordFieldFromPanel(confirmPasswordPanel);
        String password        = pf  != null ? new String(pf.getPassword()).trim()  : "";
        String confirmPassword = cpf != null ? new String(cpf.getPassword()).trim() : "";

        statusLabel.setForeground(UITheme.RED);
        statusLabel.setText(" ");

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()
                || address.isEmpty() || phone.isEmpty() || ville.isEmpty()) {
            statusLabel.setText("Veuillez remplir tous les champs.");
            return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            statusLabel.setText("Email invalide.");
            return;
        }

        // ⭐ Validation complète via PasswordValidator (du code ami)
        ValidationResult validation = PasswordValidator.validate(password);
        if (!validation.isValid()) {
            String errors = String.join("\n", validation.getErrors());
            JOptionPane.showMessageDialog(this,
                    "❌ Mot de passe non conforme :\n\n" + errors,
                    "Mot de passe invalide", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Les mots de passe ne correspondent pas.");
            if (pf  != null) pf.setText("");
            if (cpf != null) cpf.setText("");
            return;
        }

        if (!clientService.connect()) {
            statusLabel.setText("Serveur inaccessible.");
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

        String response = clientService.register(
                nom, prenom, email, hashedPassword, address, phone, ville);

        if ("REGISTER_SUCCESS_OTP_SENT".equals(response)) {
            JOptionPane.showMessageDialog(this,
                    "Compte créé !\nUn code OTP a été envoyé à votre email.",
                    "Vérification requise", JOptionPane.INFORMATION_MESSAGE);
            setVisible(false);
            new OtpFrame(clientService, email, backFrame).setVisible(true);
            dispose();

        } else if ("ERROR:EMAIL_ALREADY_EXISTS".equals(response)) {
            statusLabel.setText("Cet email est déjà utilisé.");

        } else if ("REGISTER_SUCCESS_BUT_OTP_FAILED".equals(response)) {
            JOptionPane.showMessageDialog(this,
                    "Compte créé, mais l'envoi du code a échoué.\nRenvoyez le code depuis l'écran OTP.",
                    "Attention", JOptionPane.WARNING_MESSAGE);
            setVisible(false);
            new OtpFrame(clientService, email, backFrame).setVisible(true);
            dispose();

        } else {
            statusLabel.setText("Erreur : " + response);
        }
    }
}