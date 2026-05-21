package ui;

import Client.ClientSocketService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class OtpFrame extends JFrame {

    private final ClientSocketService clientService;
    private final String email;
    private final JFrame backFrame;

    private JTextField otpField;
    private JLabel statusLabel;
    private JLabel timerLabel;
    private Timer countdownTimer;
    private int secondsLeft = 600;

    public OtpFrame(ClientSocketService clientService, String email, JFrame backFrame) {
        this.clientService = clientService;
        this.email         = email;
        this.backFrame     = backFrame;
        initUI();
        startCountdown();
    }

    private void initUI() {
        setTitle("ChriOnline — Vérification Email");
        setSize(620, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
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
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(UITheme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(420, 420));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(36, 44, 36, 44));

        // Icône email animée
        JLabel icon = new JLabel("📧");
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setFont(new Font("SansSerif", Font.PLAIN, 46));

        JLabel title = new JLabel("Vérification Email");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(UITheme.TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JLabel subtitle = new JLabel("Code envoyé à : " + email);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(UITheme.MUTED);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Timer badge
        timerLabel = new JLabel("10:00");
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerLabel.setForeground(UITheme.GREEN);
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        timerLabel.setOpaque(true);
        timerLabel.setBackground(new Color(20, 50, 30));
        timerLabel.setBorder(new EmptyBorder(4, 16, 4, 16));

        // OTP Field — grand et centré
        otpField = new JTextField();
        otpField.setBackground(UITheme.INPUT_BG);
        otpField.setForeground(UITheme.GOLD);
        otpField.setCaretColor(UITheme.GOLD);
        otpField.setFont(new Font("Segoe UI", Font.BOLD, 30));
        otpField.setHorizontalAlignment(JTextField.CENTER);
        otpField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(12, 20, 12, 20)
        ));
        otpField.setMaximumSize(new Dimension(332, 62));
        otpField.setPreferredSize(new Dimension(332, 62));
        otpField.setAlignmentX(Component.CENTER_ALIGNMENT);
        otpField.putClientProperty("JTextField.placeholderText", "• • • • • •");

        // Status
        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setForeground(UITheme.RED);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Boutons
        JButton verifyBtn = UITheme.primaryButton("Vérifier le code");
        verifyBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        verifyBtn.setMaximumSize(new Dimension(332, 46));
        verifyBtn.setPreferredSize(new Dimension(332, 46));

        JButton resendBtn = UITheme.blueButton("Renvoyer le code");
        resendBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        resendBtn.setMaximumSize(new Dimension(332, 40));
        resendBtn.setPreferredSize(new Dimension(332, 40));

        JButton backBtn = new JButton("← Retour au login");
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backBtn.setMaximumSize(new Dimension(332, 36));
        backBtn.setBackground(new Color(30, 40, 58));
        backBtn.setForeground(UITheme.MUTED);
        backBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        backBtn.setFocusPainted(false);
        backBtn.setBorderPainted(false);
        backBtn.setOpaque(true);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.setBorder(new EmptyBorder(8, 14, 8, 14));

        card.add(icon);
        card.add(Box.createVerticalStrut(10));
        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(14));
        card.add(timerLabel);
        card.add(Box.createVerticalStrut(20));
        card.add(otpField);
        card.add(Box.createVerticalStrut(10));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(verifyBtn);
        card.add(Box.createVerticalStrut(10));
        card.add(resendBtn);
        card.add(Box.createVerticalStrut(8));
        card.add(backBtn);

        root.add(card);
        setContentPane(root);

        verifyBtn.addActionListener(e -> verifyCode());
        resendBtn.addActionListener(e -> resendCode());
        otpField.addActionListener(e -> verifyCode());
        backBtn.addActionListener(e -> {
            if (countdownTimer != null) countdownTimer.stop();
            backFrame.setVisible(true);
            dispose();
        });
    }

    private void verifyCode() {
        String code = otpField.getText().trim();
        if (code.isEmpty() || code.length() != 6) {
            statusLabel.setForeground(UITheme.RED);
            statusLabel.setText("Entrez le code à 6 chiffres.");
            return;
        }
        if (!clientService.connect()) { statusLabel.setText("Serveur inaccessible."); return; }
        String response = clientService.verifyOtp(email, code);
        if ("OTP_VERIFIED".equals(response)) {
            if (countdownTimer != null) countdownTimer.stop();
            JOptionPane.showMessageDialog(this,
                    "Email vérifié ! Vous pouvez vous connecter.",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
            new LoginFrame(clientService).setVisible(true);
            dispose();
        } else {
            statusLabel.setForeground(UITheme.RED);
            statusLabel.setText("Code incorrect ou expiré.");
            otpField.setText("");
        }
    }

    private void resendCode() {
        if (!clientService.connect()) { statusLabel.setText("Serveur inaccessible."); return; }
        String response = clientService.sendOtp(email);
        if ("OTP_SENT".equals(response)) {
            statusLabel.setForeground(UITheme.GREEN);
            statusLabel.setText("Nouveau code envoyé !");
            secondsLeft = 600;
            timerLabel.setForeground(UITheme.GREEN);
            timerLabel.setBackground(new Color(20, 50, 30));
        } else {
            statusLabel.setForeground(UITheme.RED);
            statusLabel.setText("Erreur envoi. Réessayez.");
        }
    }

    private void startCountdown() {
        countdownTimer = new Timer(1000, e -> {
            secondsLeft--;
            int min = secondsLeft / 60, sec = secondsLeft % 60;
            timerLabel.setText(String.format("%02d:%02d", min, sec));
            if (secondsLeft <= 60) {
                timerLabel.setForeground(UITheme.RED);
                timerLabel.setBackground(new Color(50, 10, 10));
            }
            if (secondsLeft <= 0) {
                countdownTimer.stop();
                timerLabel.setText("Expiré");
                statusLabel.setForeground(UITheme.RED);
                statusLabel.setText("Code expiré — cliquez sur Renvoyer.");
            }
        });
        countdownTimer.start();
    }
}