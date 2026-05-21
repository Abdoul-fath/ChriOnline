package ui;

import Client.AppSession;
import Client.ClientSocketService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ShopFrame extends LanguageAwareFrame {

    private final ClientSocketService clientService;
    private final AppSession session;

    private JTextField searchField;
    private JPanel productsContainer;
    private JLabel cartCountLabel;
    private JLabel welcomeLabel;
    private JPanel categoriesPanel;
    private JScrollPane productsScrollPane;
    private JButton cartBtn;
    private JButton logoutBtn;
    private JButton languageBtn;
    private JButton profileBtn;
    private JLabel logo;
    private JLabel resultLabel;
    private JButton searchBtn;

    private final List<String[]> allProducts   = new ArrayList<>();
    private final List<String>   allCategories = new ArrayList<>();
    private String currentCategory = "Tous";

    public ShopFrame(ClientSocketService clientService, AppSession session) {
        this.clientService = clientService;
        this.session       = session;
        initUI();
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) { filterProducts(); }
        });
        loadAllData();
        refreshCartCount();
    }

    // =========================================================
    // INIT
    // =========================================================

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("shop.title"));
        setSize(1440, 900);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        applyOrientation();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG);
        root.add(createTopBar(),  BorderLayout.NORTH);
        root.add(createBody(),    BorderLayout.CENTER);
        setContentPane(root);
    }

    private void applyOrientation() {
        boolean rtl = LanguageManager.getCurrentLanguage() == LanguageManager.Language.ARABIC;
        setComponentOrientation(rtl ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT);
    }

    // =========================================================
    // TOP BAR
    // =========================================================

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setBackground(new Color(10, 14, 24));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
                new EmptyBorder(10, 24, 10, 24)
        ));

        // Logo
        logo = new JLabel("ChriOnline");
        logo.setForeground(UITheme.GOLD);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));

        // Search
        JPanel searchWrap = new JPanel(new BorderLayout());
        searchWrap.setOpaque(false);
        searchWrap.setPreferredSize(new Dimension(380, 38));

        searchField = new JTextField();
        searchField.setBackground(new Color(22, 30, 46));
        searchField.setForeground(UITheme.TEXT);
        searchField.setCaretColor(UITheme.SKY);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));
        searchField.putClientProperty("JTextField.placeholderText",
                LanguageManager.getInstance().getText("shop.search.placeholder"));

        searchBtn = new JButton("Rechercher");
        searchBtn.setBackground(UITheme.GOLD);
        searchBtn.setForeground(new Color(10, 14, 24));
        searchBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchBtn.setFocusPainted(false);
        searchBtn.setBorderPainted(false);
        searchBtn.setOpaque(true);
        searchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchBtn.setBorder(new EmptyBorder(8, 16, 8, 16));

        searchWrap.add(searchField, BorderLayout.CENTER);
        searchWrap.add(searchBtn,  BorderLayout.EAST);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        center.setOpaque(false);
        center.add(searchWrap);

        // Right actions
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);

        String name = getDisplayName();
        welcomeLabel = new JLabel("Bonjour, " + name);
        welcomeLabel.setForeground(UITheme.MUTED);
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        cartCountLabel = new JLabel("(0)");
        cartCountLabel.setForeground(UITheme.GOLD);
        cartCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        profileBtn  = topBtn("Profil",      new Color(42, 56, 80),  UITheme.TEXT);
        cartBtn     = topBtn("Panier (0)",  UITheme.GREEN,          Color.WHITE);
        languageBtn = topBtn(
                LanguageManager.getCurrentLanguage().getFlag() + " " +
                LanguageManager.getCurrentLanguage().getDisplayName(),
                new Color(28, 38, 56), UITheme.MUTED
        );
        logoutBtn   = topBtn("Déconnexion", UITheme.RED,            Color.WHITE);

        languageBtn.addActionListener(e -> {
            JPopupMenu m = new JPopupMenu();
            m.setBackground(UITheme.CARD);
            for (LanguageManager.Language lang : LanguageManager.Language.values()) {
                JMenuItem item = new JMenuItem(lang.getFlag() + "  " + lang.getDisplayName());
                item.setBackground(UITheme.CARD);
                item.setForeground(UITheme.TEXT);
                item.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                item.addActionListener(ev -> {
                    LanguageManager.setLanguage(lang);
                    languageBtn.setText(lang.getFlag() + " " + lang.getDisplayName());
                });
                m.add(item);
            }
            m.show(languageBtn, 0, languageBtn.getHeight());
        });

        profileBtn.addActionListener(e -> {
            new ProfileFrame(clientService, session, this).setVisible(true);
            setVisible(false);
        });

        cartBtn.addActionListener(e -> {
            new CartFrame(clientService, session, this).setVisible(true);
            setVisible(false);
        });

        logoutBtn.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this,
                    "Voulez-vous vraiment vous déconnecter ?", "Déconnexion", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) { dispose(); new LoginFrame(clientService).setVisible(true); }
        });

        searchBtn.addActionListener(e -> filterProducts());
        searchField.addActionListener(e -> filterProducts());

        right.add(welcomeLabel);
        right.add(Box.createHorizontalStrut(8));
        right.add(profileBtn);
        right.add(cartBtn);
        right.add(languageBtn);
        right.add(logoutBtn);

        bar.add(logo,   BorderLayout.WEST);
        bar.add(center, BorderLayout.CENTER);
        bar.add(right,  BorderLayout.EAST);

        return bar;
    }

    private JButton topBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(7, 14, 7, 14));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Color hov = bg.equals(UITheme.GREEN) ? new Color(74, 222, 128)
                  : bg.equals(UITheme.RED)   ? new Color(252, 100, 100)
                  : bg.brighter();
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(hov); }
            public void mouseExited (MouseEvent e) { b.setBackground(bg);  }
        });
        return b;
    }

    // =========================================================
    // BODY (sidebar + products)
    // =========================================================

    private JPanel createBody() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(UITheme.BG);
        body.add(createSidebar(),      BorderLayout.WEST);
        body.add(createProductsArea(), BorderLayout.CENTER);
        return body;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(16, 22, 36));
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, UITheme.BORDER),
                new EmptyBorder(20, 14, 20, 14)
        ));

        JLabel sideTitle = new JLabel("CATÉGORIES");
        sideTitle.setForeground(UITheme.MUTED);
        sideTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        sideTitle.setBorder(new EmptyBorder(0, 4, 12, 0));

        categoriesPanel = new JPanel();
        categoriesPanel.setBackground(new Color(16, 22, 36));
        categoriesPanel.setLayout(new BoxLayout(categoriesPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(categoriesPanel);
        scroll.setBackground(new Color(16, 22, 36));
        scroll.getViewport().setBackground(new Color(16, 22, 36));
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        sidebar.add(sideTitle, BorderLayout.NORTH);
        sidebar.add(scroll,    BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel createProductsArea() {
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(UITheme.BG);
        area.setBorder(new EmptyBorder(20, 22, 20, 22));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.BG);
        header.setBorder(new EmptyBorder(0, 0, 16, 0));

        resultLabel = new JLabel("Tous les produits");
        resultLabel.setForeground(UITheme.TEXT);
        resultLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        header.add(resultLabel, BorderLayout.WEST);
        area.add(header, BorderLayout.NORTH);

        productsContainer = new JPanel();
        productsContainer.setBackground(UITheme.BG);
        productsContainer.setLayout(new GridBagLayout());

        productsScrollPane = new JScrollPane(productsContainer);
        productsScrollPane.setBorder(null);
        productsScrollPane.getViewport().setBackground(UITheme.BG);
        productsScrollPane.setBackground(UITheme.BG);
        productsScrollPane.getVerticalScrollBar().setUnitIncrement(18);
        productsScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        area.add(productsScrollPane, BorderLayout.CENTER);

        return area;
    }

    // =========================================================
    // DATA LOADING
    // =========================================================

    public void reloadProducts()              { loadAllData(); }
    public void reloadCategoriesAndProducts() { loadAllData(); }

    private void loadAllData() {
        loadProducts();
        loadCategories();
        buildCategoriesPanel(new LinkedHashSet<>(allCategories));
        filterProducts();
    }

    private void loadProducts() {
        allProducts.clear();
        String response = clientService.getProducts();
        if (response == null || response.startsWith("ERROR") || "NO_PRODUCTS".equals(response)) return;
        for (String product : response.split("\\|")) {
            String[] f = product.split(";");
            if (f.length >= 6) allProducts.add(new String[]{f[0],f[1],f[2],f[3],f[4],f[5]});
        }
    }

    private void loadCategories() {
        allCategories.clear();
        String response = clientService.getCategories();
        if (response == null || response.startsWith("ERROR") || "NO_CATEGORIES".equals(response)) return;
        for (String cat : response.split("\\|")) {
            String[] f = cat.split(";");
            if (f.length >= 2) {
                String name = f[1].trim();
                if (!name.isEmpty() && !name.equalsIgnoreCase("General")
                        && !name.equalsIgnoreCase("Toutes") && !name.equalsIgnoreCase("Tous")) {
                    allCategories.add(name);
                }
            }
        }
    }

    // =========================================================
    // CATEGORY BUTTONS
    // =========================================================

    private void buildCategoriesPanel(Set<String> categories) {
        categoriesPanel.removeAll();

        categoriesPanel.add(catBtn("Tous les produits", "Tous"));
        categoriesPanel.add(Box.createVerticalStrut(4));

        JPanel divider = new JPanel();
        divider.setBackground(UITheme.BORDER);
        divider.setMaximumSize(new Dimension(9999, 1));
        divider.setPreferredSize(new Dimension(200, 1));
        categoriesPanel.add(divider);
        categoriesPanel.add(Box.createVerticalStrut(8));

        List<String> sorted = new ArrayList<>(categories);
        sorted.sort(String::compareToIgnoreCase);
        for (String c : sorted) {
            if (!c.isBlank()) {
                categoriesPanel.add(catBtn(getCategoryIcon(c) + "  " + c, c));
                categoriesPanel.add(Box.createVerticalStrut(3));
            }
        }

        categoriesPanel.revalidate();
        categoriesPanel.repaint();
    }

    private JButton catBtn(String text, String value) {
        boolean active = currentCategory.equals(value);
        JButton btn = new JButton(text);
        btn.setBackground(active ? new Color(30, 64, 120) : new Color(16, 22, 36));
        btn.setForeground(active ? Color.WHITE : UITheme.MUTED);
        btn.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(9, 12, 9, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(9999, 40));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (!active) {
            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    btn.setBackground(new Color(22, 32, 52));
                    btn.setForeground(UITheme.TEXT);
                }
                public void mouseExited(MouseEvent e) {
                    if (!currentCategory.equals(value)) {
                        btn.setBackground(new Color(16, 22, 36));
                        btn.setForeground(UITheme.MUTED);
                    }
                }
            });
        }

        btn.addActionListener(e -> {
            currentCategory = value;
            buildCategoriesPanel(new LinkedHashSet<>(allCategories));
            filterProducts();
        });
        return btn;
    }

    private String getCategoryIcon(String c) {
        String l = c.toLowerCase();
        if (l.contains("tablette") || l.contains("phone")) return "📱";
        if (l.contains("inform")   || l.contains("laptop") || l.contains("ordi")) return "💻";
        if (l.contains("audio"))   return "🎧";
        if (l.contains("gaming"))  return "🎮";
        if (l.contains("access"))  return "🔌";
        if (l.contains("vetement")) return "👕";
        return "📦";
    }

    // =========================================================
    // PRODUCTS RENDERING
    // =========================================================

    private void renderProducts(List<String[]> list) {
        productsContainer.removeAll();

        if (list.isEmpty()) {
            productsContainer.setLayout(new BorderLayout());

            JPanel empty = new JPanel();
            empty.setBackground(UITheme.BG);
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));

            JLabel ico = new JLabel("🔍");
            ico.setFont(new Font("SansSerif", Font.PLAIN, 48));
            ico.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel msg = new JLabel(
                    LanguageManager.getInstance().getText("shop.empty")
            );
            msg.setForeground(UITheme.MUTED);
            msg.setFont(new Font("Segoe UI", Font.BOLD, 16));
            msg.setAlignmentX(Component.CENTER_ALIGNMENT);

            empty.add(Box.createVerticalGlue());
            empty.add(ico);
            empty.add(Box.createVerticalStrut(10));
            empty.add(msg);
            empty.add(Box.createVerticalGlue());

            productsContainer.add(empty, BorderLayout.CENTER);
        } else {

            productsContainer.setLayout(new GridBagLayout());

            int cols = getResponsiveColumns();

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.weightx = 1;

            int row = 0;
            int col = 0;

            for (String[] p : list) {

                ProductCard card = new ProductCard(
                        Integer.parseInt(p[0]),
                        p[1],
                        Double.parseDouble(p[2]),
                        p[3],
                        p[4],
                        Integer.parseInt(p[5]),
                        clientService,
                        session,
                        this::refreshCartCount
                );

                card.setPreferredSize(new Dimension(260, 390));

                gbc.gridx = col;
                gbc.gridy = row;

                productsContainer.add(card, gbc);

                col++;

                if (col >= cols) {
                    col = 0;
                    row++;
                }
            }

            gbc.gridx = 0;
            gbc.gridy = row + 1;
            gbc.weighty = 1;

            productsContainer.add(Box.createVerticalGlue(), gbc);
        }

        productsContainer.revalidate();
        productsContainer.repaint();
    }

    private int getResponsiveColumns() {
        int w = productsScrollPane.getViewport().getWidth();

        if (w >= 1350) return 4;
        if (w >= 1000) return 3;
        if (w >= 700) return 2;

        return 1;
    }

    private void filterProducts() {
        String key = searchField.getText().trim().toLowerCase();
        List<String[]> filtered = new ArrayList<>();
        for (String[] p : allProducts) {
            boolean catMatch  = currentCategory.equals("Tous") || p[4].equalsIgnoreCase(currentCategory);
            boolean nameMatch = key.isEmpty() || p[1].toLowerCase().contains(key) || p[4].toLowerCase().contains(key);
            if (catMatch && nameMatch) filtered.add(p);
        }
        renderProducts(filtered);
        resultLabel.setText(filtered.size() + " produit" + (filtered.size() > 1 ? "s" : ""));
    }

    // =========================================================
    // CART COUNT
    // =========================================================

    public void refreshCartCount() {
        SwingUtilities.invokeLater(() -> {
            String response = clientService.getCart(session.getClientId());
            if ("CART_EMPTY".equals(response) || response == null || response.startsWith("ERROR")) {
                cartBtn.setText("Panier (0)");
                return;
            }
            for (String part : response.split("\\|")) {
                if (part.startsWith("Items=")) {
                    String count = part.substring("Items=".length());
                    cartBtn.setText("Panier (" + count + ")");
                    return;
                }
            }
            cartBtn.setText("Panier (0)");
        });
    }

    // =========================================================
    // LANGUAGE
    // =========================================================

    private String getDisplayName() {
        String n = session.getFullName();
        if (n == null || n.isBlank()) return "Utilisateur";
        String[] parts = n.trim().split("\\s+");
        return parts[0];
    }

    @Override
    public void refreshTexts() {
        setTitle(LanguageManager.getInstance().getText("shop.title"));
        welcomeLabel.setText(LanguageManager.getInstance().getText("shop.welcome") + ", " + getDisplayName());
        searchBtn.setText(LanguageManager.getInstance().getText("shop.search"));
        searchField.putClientProperty("JTextField.placeholderText",
                LanguageManager.getInstance().getText("shop.search.placeholder"));
        applyOrientation();
        buildCategoriesPanel(new LinkedHashSet<>(allCategories));
        filterProducts();
        for (Component c : productsContainer.getComponents()) {
            if (c instanceof ProductCard pc) pc.refreshTexts();
        }
        revalidate();
        repaint();
    }
}