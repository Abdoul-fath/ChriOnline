package ui.components;

import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdminSidebar extends JPanel {

    public interface NavigationListener {
        void onNavigate(String pageId);
    }

    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final NavigationListener listener;
    private String activePage = "dashboard";

    // Nav items: pageId → label
    private static final String[][] NAV_ITEMS = {
        {"dashboard",     "Dashboard"},
        {"products",      "Produits"},
        {"categories",    "Catégories"},
        {"orders",        "Commandes"},
        {"users",         "Utilisateurs"},
        {"notifications", "Notifications"},
        {"stockHistory",  "Historique Stock"},
        {"statistics",    "Statistiques"},
    };

    public AdminSidebar(NavigationListener listener) {
        this.listener = listener;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(UITheme.SIDEBAR_BG);
        setPreferredSize(new Dimension(220, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UITheme.BORDER));

        // ── Logo / header ──
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(26, 20, 20, 20));

        JLabel logo = new JLabel("ChriOnline");
        logo.setForeground(UITheme.SKY);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JLabel sub = new JLabel("Admin Dashboard");
        sub.setForeground(UITheme.TEXT_MUTED);
        sub.setFont(UITheme.FONT_SMALL);

        // Thin accent line under logo
        JPanel accent = new JPanel();
        accent.setBackground(UITheme.SKY_DARK);
        accent.setMaximumSize(new Dimension(32, 2));
        accent.setPreferredSize(new Dimension(32, 2));
        accent.setOpaque(true);

        header.add(logo);
        header.add(Box.createVerticalStrut(3));
        header.add(sub);
        header.add(Box.createVerticalStrut(10));
        header.add(accent);

        // ── Nav ──
        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(new EmptyBorder(10, 12, 12, 12));

        // Section label
        JLabel sectionLbl = new JLabel("NAVIGATION");
        sectionLbl.setForeground(UITheme.TEXT_MUTED);
        sectionLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        sectionLbl.setBorder(new EmptyBorder(0, 4, 8, 0));
        sectionLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.add(sectionLbl);

        for (String[] item : NAV_ITEMS) {
            addNavButton(nav, item[0], item[1]);
            nav.add(Box.createVerticalStrut(3));
        }

        // ── Footer ──
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 18, 20, 18));

        JLabel note = new JLabel("Dark Sky Theme");
        note.setForeground(UITheme.TEXT_MUTED);
        note.setFont(UITheme.FONT_SMALL);
        footer.add(note, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);
        add(nav,    BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        setActivePage(activePage);
    }

    private void addNavButton(JPanel parent, String pageId, String label) {
        JButton btn = new JButton(label);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setBackground(UITheme.SIDEBAR_BG);
        btn.setForeground(UITheme.TEXT_SECONDARY);
        btn.setFont(UITheme.FONT_BODY);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setPreferredSize(new Dimension(196, 40));
        btn.setBorder(new EmptyBorder(9, 14, 9, 14));

        btn.addActionListener(e -> {
            setActivePage(pageId);
            if (listener != null) listener.onNavigate(pageId);
        });

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!pageId.equals(activePage)) btn.setBackground(UITheme.CARD_BG);
            }
            public void mouseExited(MouseEvent e) {
                if (!pageId.equals(activePage)) btn.setBackground(UITheme.SIDEBAR_BG);
            }
        });

        navButtons.put(pageId, btn);
        parent.add(btn);
    }

    public void setActivePage(String pageId) {
        this.activePage = pageId;
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean active = entry.getKey().equals(pageId);
            JButton btn = entry.getValue();
            if (active) {
                btn.setBackground(new Color(30, 64, 120));
                btn.setForeground(UITheme.SKY);
                btn.setFont(UITheme.FONT_BODY_BOLD);
            } else {
                btn.setBackground(UITheme.SIDEBAR_BG);
                btn.setForeground(UITheme.TEXT_SECONDARY);
                btn.setFont(UITheme.FONT_BODY);
            }
        }
    }
}