package ui.components;

import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminTopbar extends JPanel {

    public interface TopbarActionListener {
        void onRefresh();
        void onLogout();
        void onOpenNotifications();
    }

    private final JLabel titleLabel    = new JLabel("Dashboard");
    private final JLabel subtitleLabel = new JLabel("Vue d'ensemble du système");
    private final JLabel badgeLabel    = new JLabel("0");

    public AdminTopbar(TopbarActionListener listener) {
        setLayout(new BorderLayout());
        setBackground(UITheme.TOPBAR_BG);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER));
        setPreferredSize(new Dimension(0, 72));

        // ── Left: page title ──
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(14, 22, 10, 20));

        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        subtitleLabel.setForeground(UITheme.TEXT_MUTED);
        subtitleLabel.setFont(UITheme.FONT_SMALL);

        left.add(titleLabel);
        left.add(Box.createVerticalStrut(2));
        left.add(subtitleLabel);

        // ── Right: actions ──
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 16));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(0, 0, 0, 16));

        JButton notifBtn   = buildTopBtn("Notifications", UITheme.CARD_BG_ALT, UITheme.TEXT_PRIMARY);
        JButton refreshBtn = buildTopBtn("Actualiser",    UITheme.SKY_DARK,    Color.WHITE);
        JButton logoutBtn  = buildTopBtn("Déconnexion",   UITheme.DANGER,      Color.WHITE);

        // Badge notifications
        badgeLabel.setOpaque(true);
        badgeLabel.setBackground(new Color(185, 28, 28));
        badgeLabel.setForeground(Color.WHITE);
        badgeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        badgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badgeLabel.setPreferredSize(new Dimension(26, 20));
        badgeLabel.setBorder(new EmptyBorder(2, 4, 2, 4));

        notifBtn.addActionListener(e -> { if (listener != null) listener.onOpenNotifications(); });
        refreshBtn.addActionListener(e -> { if (listener != null) listener.onRefresh(); });
        logoutBtn.addActionListener(e -> { if (listener != null) listener.onLogout(); });

        right.add(notifBtn);
        right.add(badgeLabel);
        right.add(refreshBtn);
        right.add(logoutBtn);

        add(left,  BorderLayout.WEST);
        add(right, BorderLayout.EAST);
    }

    private JButton buildTopBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(UITheme.FONT_BODY_BOLD);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        return b;
    }

    public void setPageInfo(String title, String subtitle) {
        titleLabel.setText(title);
        subtitleLabel.setText(subtitle);
    }

    public void setUnreadNotificationsCount(int count) {
        badgeLabel.setText(String.valueOf(Math.max(0, count)));
    }
}