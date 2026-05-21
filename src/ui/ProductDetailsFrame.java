package ui;

import Client.AppSession;
import Client.ClientSocketService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ProductDetailsFrame extends JFrame {

    private final int productId;

    public ProductDetailsFrame(ClientSocketService clientService, AppSession session,
                                int productId, Runnable onCartChanged) {
        this.productId = productId;

        setTitle("Détails du produit");
        setSize(920, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Loading screen
        JPanel loading = new JPanel(new GridBagLayout());
        loading.setBackground(UITheme.BG);
        JLabel lbl = new JLabel("Chargement...");
        lbl.setForeground(UITheme.MUTED);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        loading.add(lbl);
        setContentPane(loading);

        new SwingWorker<Boolean, Void>() {
            String name = "", description = "", image = "", category = "";
            double price = 0;
            int stock = 0;

            @Override protected Boolean doInBackground() {
                try {
                    String response = clientService.getProduct(productId);
                    if (response == null || response.startsWith("ERROR")) return false;
                    String[] parts = response.split(";");
                    if (parts.length < 7) return false;
                    name        = parts[1];
                    price       = Double.parseDouble(parts[2]);
                    description = parts[3];
                    stock       = Integer.parseInt(parts[4]);
                    image       = parts[5];
                    category    = parts[6];
                    return true;
                } catch (Exception e) { e.printStackTrace(); return false; }
            }

            @Override protected void done() {
                try {
                    if (!get()) {
                        JOptionPane.showMessageDialog(ProductDetailsFrame.this,
                                "Impossible de charger le produit.", "Erreur", JOptionPane.ERROR_MESSAGE);
                        dispose();
                        return;
                    }
                    buildUI(name, price, description, stock, image, category, clientService, session, onCartChanged);
                } catch (Exception e) {
                    e.printStackTrace();
                    dispose();
                }
            }
        }.execute();
    }

    private void buildUI(String name, double price, String description, int stock,
                         String image, String category,
                         ClientSocketService clientService, AppSession session, Runnable onCartChanged) {

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(13,17,27), getWidth(), getHeight(), new Color(18,26,44)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBackground(UITheme.BG);
        root.setBorder(new EmptyBorder(24, 28, 24, 28));

        // ── Left: image ──
        JPanel imageCard = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(UITheme.BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.dispose();
            }
        };
        imageCard.setOpaque(false);
        imageCard.setPreferredSize(new Dimension(320, 0));
        imageCard.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel imageLabel = new JLabel(UITheme.loadProductImage(image, 270, 270));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageCard.add(imageLabel, BorderLayout.CENTER);

        // ── Right: info ──
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(new EmptyBorder(0, 28, 0, 0));

        // Category badge
        JLabel catBadge = new JLabel(category);
        catBadge.setForeground(UITheme.SKY);
        catBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        catBadge.setOpaque(true);
        catBadge.setBackground(new Color(12, 50, 80));
        catBadge.setBorder(new EmptyBorder(3, 10, 3, 10));
        catBadge.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel("<html>" + name + "</html>");
        nameLabel.setForeground(UITheme.TEXT);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priceLabel = new JLabel(String.format("%.2f DH", price));
        priceLabel.setForeground(UITheme.GOLD);
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Stock badge
        JLabel stockBadge = buildStockBadge(stock);
        stockBadge.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Divider
        JPanel div = new JPanel();
        div.setBackground(UITheme.BORDER);
        div.setMaximumSize(new Dimension(9999, 1));
        div.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Description
        JLabel descTitle = new JLabel("Description");
        descTitle.setForeground(UITheme.MUTED);
        descTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        descTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea descArea = new JTextArea(
                (description != null && !description.isBlank())
                        ? description
                        : LanguageManager.getInstance().getText("product.no.description")
        );
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBackground(UITheme.INPUT_BG);
        descArea.setForeground(UITheme.TEXT);
        descArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        descArea.setMaximumSize(new Dimension(9999, 100));
        descArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Qty row
        JPanel qtyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        qtyRow.setOpaque(false);
        qtyRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel qtyLbl = new JLabel("Quantité :");
        qtyLbl.setForeground(UITheme.MUTED);
        qtyLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        SpinnerNumberModel spinModel = new SpinnerNumberModel(1, 1, Math.max(stock, 1), 1);
        JSpinner spinner = new JSpinner(spinModel);
        spinner.setPreferredSize(new Dimension(72, 36));

        qtyRow.add(qtyLbl);
        qtyRow.add(spinner);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton addBtn   = UITheme.primaryButton("Ajouter au panier");
        JButton closeBtn = UITheme.blueButton("Fermer");

        if (stock == 0) {
            addBtn.setEnabled(false);
            addBtn.setBackground(new Color(40, 40, 50));
            addBtn.setForeground(UITheme.MUTED);
            addBtn.setText("Rupture de stock");
        }

        btnRow.add(addBtn);
        btnRow.add(closeBtn);

        info.add(catBadge);
        info.add(Box.createVerticalStrut(10));
        info.add(nameLabel);
        info.add(Box.createVerticalStrut(8));
        info.add(priceLabel);
        info.add(Box.createVerticalStrut(10));
        info.add(stockBadge);
        info.add(Box.createVerticalStrut(16));
        info.add(div);
        info.add(Box.createVerticalStrut(14));
        info.add(descTitle);
        info.add(Box.createVerticalStrut(6));
        info.add(descArea);
        info.add(Box.createVerticalStrut(20));
        info.add(qtyRow);
        info.add(Box.createVerticalStrut(14));
        info.add(btnRow);

        root.add(imageCard, BorderLayout.WEST);
        root.add(info,      BorderLayout.CENTER);

        setContentPane(root);
        revalidate();
        repaint();

        addBtn.addActionListener(e -> {
            int qty = (Integer) spinner.getValue();
            String response = clientService.addToCart(session.getClientId(), productId, qty);
            if ("CART_ADD_SUCCESS".equals(response)) {
                addBtn.setText("✓ Ajouté !");
                addBtn.setBackground(new Color(22, 163, 74));
                Timer t = new Timer(1800, ev -> {
                    addBtn.setText("Ajouter au panier");
                    addBtn.setBackground(UITheme.GREEN);
                });
                t.setRepeats(false);
                t.start();
                if (onCartChanged != null) onCartChanged.run();
            } else {
                JOptionPane.showMessageDialog(this, "Erreur : " + response);
            }
        });

        closeBtn.addActionListener(e -> dispose());
    }

    private JLabel buildStockBadge(int stock) {
        JLabel badge = new JLabel();
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setOpaque(true);
        badge.setBorder(new EmptyBorder(3, 10, 3, 10));

        if (stock > 10) {
            badge.setText("En stock (" + stock + " disponibles)");
            badge.setBackground(new Color(20, 83, 45));
            badge.setForeground(new Color(134, 239, 172));
        } else if (stock > 0) {
            badge.setText("Stock limité — " + stock + " restant" + (stock > 1 ? "s" : ""));
            badge.setBackground(new Color(78, 50, 0));
            badge.setForeground(new Color(253, 186, 116));
        } else {
            badge.setText("Rupture de stock");
            badge.setBackground(new Color(69, 10, 10));
            badge.setForeground(new Color(252, 165, 165));
        }
        return badge;
    }
}