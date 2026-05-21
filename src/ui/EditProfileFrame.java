package ui;

import Client.AppSession;
import Client.ClientSocketService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class EditProfileFrame extends LanguageAwareFrame {

    private final ClientSocketService clientService;
    private final AppSession session;
    private final ProfileFrame parentFrame;

    private JTextField nameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextField addressField;
    private JTextField cityField;
    private JLabel titleLabel;
    private JLabel statusLabel;

    public EditProfileFrame(ClientSocketService clientService, AppSession session, ProfileFrame parentFrame) {
        this.clientService = clientService;
        this.session       = session;
        this.parentFrame   = parentFrame;
        initUI();
        loadUserData();
    }

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("profile.edit.title"));
        setSize(500, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(13, 17, 27), getWidth(), getHeight(), new Color(18, 26, 44)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBackground(UITheme.BG);

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(UITheme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(420, 490));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(28, 36, 28, 36));

        titleLabel = new JLabel(LanguageManager.getInstance().getText("profile.edit.title"));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setForeground(UITheme.TEXT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JLabel subtitle = new JLabel("Modifiez vos informations personnelles");
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setForeground(UITheme.MUTED);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        nameField    = buildField(LanguageManager.getInstance().getText("profile.name"));
        emailField   = buildField(LanguageManager.getInstance().getText("profile.email"));
        phoneField   = buildField(LanguageManager.getInstance().getText("profile.phone"));
        addressField = buildField(LanguageManager.getInstance().getText("profile.address"));
        cityField    = buildField(LanguageManager.getInstance().getText("profile.city"));

        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setForeground(UITheme.RED);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(9999, 46));

        JButton saveBtn   = UITheme.primaryButton(LanguageManager.getInstance().getText("profile.save"));
        JButton cancelBtn = UITheme.blueButton(LanguageManager.getInstance().getText("profile.cancel"));

        btnRow.add(saveBtn);
        btnRow.add(cancelBtn);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(22));
        card.add(labelFor(LanguageManager.getInstance().getText("profile.name")));
        card.add(Box.createVerticalStrut(4));
        card.add(nameField);
        card.add(Box.createVerticalStrut(12));
        card.add(labelFor(LanguageManager.getInstance().getText("profile.email")));
        card.add(Box.createVerticalStrut(4));
        card.add(emailField);
        card.add(Box.createVerticalStrut(12));
        card.add(labelFor(LanguageManager.getInstance().getText("profile.phone")));
        card.add(Box.createVerticalStrut(4));
        card.add(phoneField);
        card.add(Box.createVerticalStrut(12));
        card.add(labelFor(LanguageManager.getInstance().getText("profile.address")));
        card.add(Box.createVerticalStrut(4));
        card.add(addressField);
        card.add(Box.createVerticalStrut(12));
        card.add(labelFor(LanguageManager.getInstance().getText("profile.city")));
        card.add(Box.createVerticalStrut(4));
        card.add(cityField);
        card.add(Box.createVerticalStrut(10));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(btnRow);

        root.add(card);
        setContentPane(root);

        saveBtn.addActionListener(e -> saveProfile());
        cancelBtn.addActionListener(e -> dispose());
    }

    private JLabel labelFor(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UITheme.MUTED);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField buildField(String placeholder) {
        JTextField f = new JTextField();
        f.setBackground(UITheme.INPUT_BG);
        f.setForeground(UITheme.TEXT);
        f.setCaretColor(UITheme.SKY);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setSelectedTextColor(Color.WHITE);
        f.setSelectionColor(UITheme.BLUE);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(9, 13, 9, 13)
        ));
        f.setMaximumSize(new Dimension(9999, 44));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.putClientProperty("JTextField.placeholderText", placeholder);
        return f;
    }

    private void loadUserData() {
        String response = clientService.getProfile(session.getClientId());
        if (response == null || !response.startsWith("PROFILE_DATA:")) {
            nameField.setText("Client #" + session.getClientId());
            return;
        }
        String[] parts = response.substring("PROFILE_DATA:".length()).split(";");
        nameField.setText(parts.length > 0 ? parts[0] : "");
        emailField.setText(parts.length > 1 ? parts[1] : "");
        phoneField.setText(parts.length > 2 ? parts[2] : "");
        addressField.setText(parts.length > 3 ? parts[3] : "");
        cityField.setText(parts.length > 4 ? parts[4] : "");
    }

    private void saveProfile() {
        String name    = nameField.getText().trim();
        String email   = emailField.getText().trim();
        String phone   = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String city    = cityField.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {
            statusLabel.setText("Nom et email requis.");
            return;
        }

        String response = clientService.updateProfile(session.getClientId(), name, email, phone, address, city);

        if ("UPDATE_PROFILE_SUCCESS".equals(response)) {
            JOptionPane.showMessageDialog(this,
                    LanguageManager.getInstance().getText("profile.update.success"),
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
            if (parentFrame != null) parentFrame.refreshProfile();
            dispose();
        } else {
            statusLabel.setText(LanguageManager.getInstance().getText("profile.update.error"));
        }
    }

    @Override
    public void refreshTexts() {
        setTitle(LanguageManager.getInstance().getText("profile.edit.title"));
        titleLabel.setText(LanguageManager.getInstance().getText("profile.edit.title"));
        revalidate(); repaint();
    }
}