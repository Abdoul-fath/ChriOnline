package ui;

import Client.AppSession;
import Client.ClientSocketService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ProductCard extends JPanel {

    private int quantity = 1;
    private final int productId;
    private final int stock;
    private final String productName;
    private final double price;
    private final String imagePath;
    private final String category;

    private JLabel stockLabel;
    private JButton addBtn;

    private final ClientSocketService clientService;
    private final AppSession session;
    private final Runnable onCartChanged;
    private final Runnable onOpenDetails;

    private boolean hovered = false;

    public ProductCard(int productId, String productName, double price, String imagePath,
                       String category, int stock,
                       ClientSocketService clientService, AppSession session,
                       Runnable onCartChanged, Runnable onOpenDetails) {
        this.productId     = productId;
        this.productName   = productName;
        this.price         = price;
        this.imagePath     = imagePath;
        this.category      = category;
        this.stock         = stock;
        this.clientService = clientService;
        this.session       = session;
        this.onCartChanged = onCartChanged;
        this.onOpenDetails = onOpenDetails;
        initUI();
    }

    public ProductCard(int productId, String productName, double price, String imagePath,
                       String category, int stock,
                       ClientSocketService clientService, AppSession session, Runnable onCartChanged) {
        this(productId, productName, price, imagePath, category, stock, clientService, session, onCartChanged, null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(hovered ? UITheme.CARD_2 : UITheme.CARD);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
        g2.setColor(hovered ? UITheme.BLUE : UITheme.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g2.dispose();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 0, 0));
        setPreferredSize(new Dimension(260, 320));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Image area
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBackground(new Color(16, 22, 36));
        imagePanel.setPreferredSize(new Dimension(0, 148));
        imagePanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel imageLabel = new JLabel(UITheme.loadProductImage(imagePath, 124, 124));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imagePanel.add(imageLabel, BorderLayout.CENTER);

        // Stock badge
        JLabel stockBadge = buildStockBadge();
        JPanel imageWrapper = new JPanel(new BorderLayout());
        imageWrapper.setOpaque(false);
        imageWrapper.add(imagePanel, BorderLayout.CENTER);

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        badgeRow.setOpaque(false);
        badgeRow.add(stockBadge);
        imageWrapper.add(badgeRow, BorderLayout.SOUTH);

        // Info area
        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(new EmptyBorder(10, 14, 12, 14));

        JLabel nameLabel = new JLabel("<html><b>" + productName + "</b></html>");
        nameLabel.setForeground(UITheme.TEXT);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel categoryLabel = new JLabel(category);
        categoryLabel.setForeground(UITheme.MUTED);
        categoryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        categoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priceLabel = new JLabel(String.format("%.2f DH", price));
        priceLabel.setForeground(UITheme.GOLD);
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Qty + Add row
        JPanel qtyRow = new JPanel(new BorderLayout(6, 0));
        qtyRow.setOpaque(false);
        qtyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        qtyRow.setMaximumSize(new Dimension(9999, 34));

        JPanel qtyCtrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        qtyCtrl.setOpaque(false);

        JButton minusBtn = qtyBtn("−");
        JButton plusBtn  = qtyBtn("+");

        JLabel qtyDisplay = new JLabel("1");
        qtyDisplay.setForeground(UITheme.TEXT);
        qtyDisplay.setFont(new Font("Segoe UI", Font.BOLD, 13));
        qtyDisplay.setPreferredSize(new Dimension(20, 28));
        qtyDisplay.setHorizontalAlignment(SwingConstants.CENTER);

        minusBtn.addActionListener(e -> {
            if (quantity > 1) { quantity--; qtyDisplay.setText(String.valueOf(quantity)); }
        });
        plusBtn.addActionListener(e -> {
            if (quantity < stock) { quantity++; qtyDisplay.setText(String.valueOf(quantity)); }
        });

        qtyCtrl.add(minusBtn);
        qtyCtrl.add(qtyDisplay);
        qtyCtrl.add(plusBtn);

        addBtn = stock == 0 ? buildOutOfStockBtn() : buildAddBtn();

        qtyRow.add(qtyCtrl, BorderLayout.WEST);
        qtyRow.add(addBtn, BorderLayout.CENTER);

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(2));
        infoPanel.add(categoryLabel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(priceLabel);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(qtyRow);

        add(imageWrapper, BorderLayout.NORTH);
        add(infoPanel, BorderLayout.CENTER);

        // Hover + click
        MouseAdapter hover = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            public void mouseClicked(MouseEvent e) { openProductDetails(); }
        };
        addMouseListener(hover);
        imagePanel.addMouseListener(hover);
        nameLabel.addMouseListener(hover);
    }

    private JButton qtyBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(30, 42, 60));
        b.setForeground(UITheme.TEXT);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(28, 28));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton buildAddBtn() {
        JButton btn = new JButton("+ Panier");
        btn.setBackground(UITheme.GREEN);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 12, 6, 12));

        btn.addActionListener(e -> {
            String response = clientService.addToCart(session.getClientId(), productId, quantity);
            if ("CART_ADD_SUCCESS".equals(response)) {
                btn.setText("✓ Ajouté");
                btn.setBackground(new Color(22, 163, 74));
                Timer t = new Timer(1500, ev -> {
                    btn.setText("+ Panier");
                    btn.setBackground(UITheme.GREEN);
                });
                t.setRepeats(false);
                t.start();
                if (onCartChanged != null) onCartChanged.run();
            } else {
                JOptionPane.showMessageDialog(this, "Erreur : " + response);
            }
        });
        return btn;
    }

    private JButton buildOutOfStockBtn() {
        JButton btn = new JButton("Rupture");
        btn.setBackground(new Color(40, 40, 50));
        btn.setForeground(UITheme.MUTED);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setEnabled(false);
        btn.setBorder(new EmptyBorder(6, 12, 6, 12));
        return btn;
    }

    private JLabel buildStockBadge() {
        JLabel badge = new JLabel();
        badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badge.setBorder(new EmptyBorder(2, 8, 2, 8));
        badge.setOpaque(true);

        if (stock > 10) {
            badge.setText("En stock");
            badge.setBackground(new Color(20, 83, 45));
            badge.setForeground(new Color(134, 239, 172));
        } else if (stock > 0) {
            badge.setText("Stock limité : " + stock);
            badge.setBackground(new Color(78, 50, 0));
            badge.setForeground(new Color(253, 186, 116));
        } else {
            badge.setText("Rupture");
            badge.setBackground(new Color(69, 10, 10));
            badge.setForeground(new Color(252, 165, 165));
        }
        stockLabel = new JLabel(badge.getText());
        return badge;
    }

    private void openProductDetails() {
        if (onOpenDetails != null) { onOpenDetails.run(); return; }
        new ProductDetailsFrame(clientService, session, productId, onCartChanged).setVisible(true);
    }

    public void refreshTexts() {
        if (stock > 0 && addBtn != null) addBtn.setText("+ Panier");
    }

    public int getProductId() { return productId; }
}