package ui;

import Client.ClientSocketService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

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

    public RegisterFrame(ClientSocketService clientService, JFrame backFrame) {
        this.clientService = clientService;
        this.backFrame = backFrame;
        initUI();
    }

    private void initUI() {
        setTitle("📝 ChriOnline - Inscription");
        setSize(800, 860);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new GridBagLayout());

        JPanel card = UITheme.cardPanel();
        card.setPreferredSize(new Dimension(500, 760));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(22, 35, 22, 35)
        ));

        JLabel title = new JLabel("📝 Créer un compte");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));

        JLabel subtitle = new JLabel("Inscription client");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(UITheme.MUTED);
        subtitle.setFont(UITheme.normalFont());

        nomField = createStyledTextField("👤 Nom");
        prenomField = createStyledTextField("👤 Prénom");
        emailField = createStyledTextField("📧 Email");

        passwordPanel = UITheme.createPasswordFieldWithEye("🔒 Mot de passe");
        passwordPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordPanel.setMaximumSize(new Dimension(340, 60));
        passwordPanel.setPreferredSize(new Dimension(340, 60));

        confirmPasswordPanel = UITheme.createPasswordFieldWithEye("🔒 Confirmer le mot de passe");
        confirmPasswordPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmPasswordPanel.setMaximumSize(new Dimension(340, 60));
        confirmPasswordPanel.setPreferredSize(new Dimension(340, 60));

        addressField = createStyledTextField("🏠 Adresse");
        phoneField = createStyledTextField("📱 Téléphone");
        villeField = createStyledTextField("🏙️ Ville");

        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setForeground(UITheme.RED);
        statusLabel.setFont(UITheme.smallFont());

        JButton registerBtn = UITheme.primaryButton("✅ S'INSCRIRE");
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerBtn.setMaximumSize(new Dimension(340, 45));
        registerBtn.setPreferredSize(new Dimension(340, 45));

        JButton backBtn = UITheme.blueButton("← RETOUR");
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backBtn.setMaximumSize(new Dimension(340, 45));
        backBtn.setPreferredSize(new Dimension(340, 45));

        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(18));
        card.add(nomField);
        card.add(Box.createVerticalStrut(10));
        card.add(prenomField);
        card.add(Box.createVerticalStrut(10));
        card.add(emailField);
        card.add(Box.createVerticalStrut(10));
        card.add(passwordPanel);
        card.add(Box.createVerticalStrut(10));
        card.add(confirmPasswordPanel);
        card.add(Box.createVerticalStrut(10));
        card.add(addressField);
        card.add(Box.createVerticalStrut(10));
        card.add(phoneField);
        card.add(Box.createVerticalStrut(10));
        card.add(villeField);
        card.add(Box.createVerticalStrut(12));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(registerBtn);
        card.add(Box.createVerticalStrut(12));
        card.add(backBtn);

        root.add(card);
        setContentPane(root);

        registerBtn.addActionListener(e -> register());
        backBtn.addActionListener(e -> {
            backFrame.setVisible(true);
            dispose();
        });
    }

    private JTextField createStyledTextField(String title) {
        JTextField field = UITheme.textField();
        field.setMaximumSize(new Dimension(340, 48));
        field.setPreferredSize(new Dimension(340, 48));
        field.setBorder(UITheme.titledBorder(title));
        return field;
    }

    private void register() {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();

        JPasswordField passwordField = UITheme.getPasswordFieldFromPanel(passwordPanel);
        JPasswordField confirmPasswordField = UITheme.getPasswordFieldFromPanel(confirmPasswordPanel);

        String password = new String(passwordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

        String address = addressField.getText().trim();
        String phone = phoneField.getText().trim();
        String ville = villeField.getText().trim();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()
                || address.isEmpty() || phone.isEmpty() || ville.isEmpty()) {
            statusLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            statusLabel.setText("Email invalide.");
            return;
        }

        if (password.length() < 6) {
            statusLabel.setText("Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Les mots de passe ne correspondent pas.");
            passwordField.setText("");
            confirmPasswordField.setText("");
            return;
        }

        if (!clientService.connect()) {
            statusLabel.setText("Serveur inaccessible.");
            return;
        }

        String response = clientService.register(
                nom, prenom, email, password, address, phone, ville
        );

        if ("REGISTER_SUCCESS_OTP_SENT".equals(response)) {
            JOptionPane.showMessageDialog(this,
                    "✅ Compte créé.\nUn code OTP a été envoyé à votre email.",
                    "Vérification requise", JOptionPane.INFORMATION_MESSAGE);

            setVisible(false);
            new OtpFrame(clientService, email, backFrame).setVisible(true);
            dispose();

        } else if ("ERROR:EMAIL_ALREADY_EXISTS".equals(response)) {
            statusLabel.setText("Cet email est déjà utilisé.");
        } else if ("REGISTER_SUCCESS_BUT_OTP_FAILED".equals(response)) {
            JOptionPane.showMessageDialog(this,
                    "Compte créé, mais l'envoi du code a échoué.\nEssayez de vous reconnecter puis renvoyez le code.",
                    "Attention", JOptionPane.WARNING_MESSAGE);

            setVisible(false);
            new OtpFrame(clientService, email, backFrame).setVisible(true);
            dispose();
        } else {
            statusLabel.setText("Erreur : " + response);
        }
    }
}