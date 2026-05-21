package ui;

import Client.AppSession;
import Client.ClientSocketService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ProfileFrame extends LanguageAwareFrame {

    private final ClientSocketService clientService;
    private final AppSession session;
    private final JFrame backFrame;

    private JLabel welcomeLabel;
    private JLabel nameLabel;
    private JLabel emailLabel;
    private JLabel phoneLabel;
    private JLabel addressLabel;
    private JLabel cityLabel;
    private JButton editBtn;
    private JButton ordersBtn;
    private JButton backBtn;
    private JLabel titleLabel;

    public ProfileFrame(ClientSocketService clientService, AppSession session, JFrame backFrame) {
        this.clientService = clientService;
        this.session       = session;
        this.backFrame     = backFrame;
        initUI();
        refreshProfile();
    }

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("profile.title"));
        setSize(580, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 14)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(13, 17, 27), getWidth(), getHeight(), new Color(18, 26, 44)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBackground(UITheme.BG);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));

        // ── Header ──
        JPanel header = buildCard();
        header.setLayout(new BorderLayout(12, 0));
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Avatar circle
        JPanel avatar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BLUE);
                g2.fillOval(0, 0, 52, 52);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                FontMetrics fm = g2.getFontMetrics();
                String initials = getInitials();
                g2.drawString(initials, (52 - fm.stringWidth(initials)) / 2, (52 + fm.getAscent()) / 2 - 2);
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(52, 52));

        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));

        titleLabel = new JLabel(LanguageManager.getInstance().getText("profile.title"));
        titleLabel.setForeground(UITheme.TEXT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        welcomeLabel = new JLabel("Bienvenue !");
        welcomeLabel.setForeground(UITheme.MUTED);
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        headerText.add(titleLabel);
        headerText.add(Box.createVerticalStrut(2));
        headerText.add(welcomeLabel);

        header.add(avatar, BorderLayout.WEST);
        header.add(headerText, BorderLayout.CENTER);

        // ── Info panel ──
        JPanel infoCard = buildCard();
        infoCard.setLayout(new GridLayout(5, 1, 0, 0));
        infoCard.setBorder(new EmptyBorder(8, 20, 8, 20));

        nameLabel    = new JLabel("---");
        emailLabel   = new JLabel("---");
        phoneLabel   = new JLabel("---");
        addressLabel = new JLabel("---");
        cityLabel    = new JLabel("---");

        infoCard.add(infoRow("Nom complet",  nameLabel));
        infoCard.add(infoRow("Email",        emailLabel));
        infoCard.add(infoRow("Téléphone",    phoneLabel));
        infoCard.add(infoRow("Adresse",      addressLabel));
        infoCard.add(infoRow("Ville",        cityLabel));

        // ── Boutons ──
        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 12, 0));
        btnPanel.setOpaque(false);

        editBtn   = UITheme.blueButton("✏ Modifier");
        ordersBtn = UITheme.primaryButton("📦 Commandes");
        backBtn   = UITheme.dangerButton("← Retour");

        btnPanel.add(editBtn);
        btnPanel.add(ordersBtn);
        btnPanel.add(backBtn);

        root.add(header,   BorderLayout.NORTH);
        root.add(infoCard, BorderLayout.CENTER);
        root.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(root);

        editBtn.addActionListener(e   -> new EditProfileFrame(clientService, session, this).setVisible(true));
        ordersBtn.addActionListener(e -> new OrderHistoryFrame(clientService, session).setVisible(true));
        backBtn.addActionListener(e   -> { dispose(); backFrame.setVisible(true); });
    }

    private JPanel buildCard() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(UITheme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    private JPanel infoRow(String label, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
                new EmptyBorder(10, 0, 10, 0)
        ));

        JLabel lbl = new JLabel(label);
        lbl.setForeground(UITheme.MUTED);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setPreferredSize(new Dimension(130, 0));

        valueLabel.setForeground(UITheme.TEXT);
        valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        row.add(lbl, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    private String getInitials() {
        String name = nameLabel != null ? nameLabel.getText() : "?";
        if (name.isBlank() || "---".equals(name)) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) return String.valueOf(parts[0].charAt(0)).toUpperCase()
                                     + String.valueOf(parts[1].charAt(0)).toUpperCase();
        return name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
    }

    public void refreshProfile() {
        String response = clientService.getProfile(session.getClientId());

        if (response == null || response.startsWith("ERROR") || !response.startsWith("PROFILE_DATA:")) {
            welcomeLabel.setText("Client #" + session.getClientId());
            return;
        }

        String[] parts = response.substring("PROFILE_DATA:".length()).split(";");
        String fullName = parts.length > 0 ? parts[0] : "";
        String email    = parts.length > 1 ? parts[1] : "";
        String phone    = parts.length > 2 ? parts[2] : "";
        String address  = parts.length > 3 ? parts[3] : "";
        String city     = parts.length > 4 ? parts[4] : "";

        if (fullName.isBlank()) fullName = "Client #" + session.getClientId();

        welcomeLabel.setText(fullName);
        nameLabel.setText(fullName.isBlank()  ? "—" : fullName);
        emailLabel.setText(email.isBlank()    ? "—" : email);
        phoneLabel.setText(phone.isBlank()    ? "—" : phone);
        addressLabel.setText(address.isBlank() ? "—" : address);
        cityLabel.setText(city.isBlank()      ? "—" : city);

        revalidate();
        repaint();
    }

    @Override
    public void refreshTexts() {
        setTitle(LanguageManager.getInstance().getText("profile.title"));
        titleLabel.setText(LanguageManager.getInstance().getText("profile.title"));
        editBtn.setText("✏ " + LanguageManager.getInstance().getText("profile.edit"));
        ordersBtn.setText("📦 " + LanguageManager.getInstance().getText("profile.orders"));
        backBtn.setText("← " + LanguageManager.getInstance().getText("cart.back"));
        revalidate(); repaint();
    }
}