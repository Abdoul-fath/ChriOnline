package ui;

import Client.AppSession;
import Client.ClientSocketService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CartFrame extends LanguageAwareFrame {

    private final ClientSocketService clientService;
    private final AppSession session;
    private final JFrame backFrame;

    private JTable table;
    private DefaultTableModel model;
    private JLabel totalLabel;
    private JButton checkoutBtn;
    private JButton clearAllBtn;
    private JButton backBtn;
    private JLabel titleLabel;

    private final List<CartProductInfo> cartProducts = new ArrayList<>();

    public CartFrame(ClientSocketService clientService, AppSession session, JFrame backFrame) {
        this.clientService = clientService;
        this.session       = session;
        this.backFrame     = backFrame;
        initUI();
        loadCart();
    }

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("cart.title"));
        setSize(1060, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(13, 17, 27), getWidth(), getHeight(), new Color(18, 26, 44)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBackground(UITheme.BG);
        root.setBorder(new EmptyBorder(18, 20, 18, 20));

        // ── Header ──
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 14, 0));

        titleLabel = new JLabel("Panier");
        titleLabel.setForeground(UITheme.TEXT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));

        header.add(titleLabel, BorderLayout.WEST);

        // ── Table ──
        model = new DefaultTableModel(new Object[]{
                LanguageManager.getInstance().getText("cart.product"),
                LanguageManager.getInstance().getText("cart.quantity"),
                LanguageManager.getInstance().getText("cart.unit.price"),
                LanguageManager.getInstance().getText("cart.subtotal"),
                LanguageManager.getInstance().getText("cart.remove")
        }, 0) {
            public boolean isCellEditable(int row, int col) { return col == 4; }
        };

        table = new JTable(model) {
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? UITheme.CARD : UITheme.CARD_2);
                    c.setForeground(UITheme.TEXT);
                }
                return c;
            }
        };
        table.setBackground(UITheme.CARD);
        table.setForeground(UITheme.TEXT);
        table.setRowHeight(48);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setSelectionBackground(new Color(37, 99, 180));
        table.setSelectionForeground(Color.WHITE);
        table.setShowVerticalLines(false);
        table.setGridColor(UITheme.BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));

        // Header style
        JTableHeader th = table.getTableHeader();
        th.setBackground(new Color(10, 14, 24));
        th.setForeground(UITheme.MUTED);
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setPreferredSize(new Dimension(0, 38));
        th.setReorderingAllowed(false);

        // Align prices to right
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);

        // Remove button col
        String removeKey = LanguageManager.getInstance().getText("cart.remove");
        table.getColumn(removeKey).setCellRenderer(new ButtonRenderer());
        table.getColumn(removeKey).setCellEditor(new ButtonEditor());
        table.getColumn(removeKey).setPreferredWidth(120);
        table.getColumn(removeKey).setMaxWidth(120);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1, true));
        scroll.setBackground(UITheme.CARD);
        scroll.getViewport().setBackground(UITheme.CARD);

        // ── Footer ──
        JPanel footer = new JPanel(new BorderLayout(0, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(14, 0, 0, 0));

        totalLabel = new JLabel("Total : 0.00 DH");
        totalLabel.setForeground(UITheme.GOLD);
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);

        backBtn     = UITheme.blueButton("← Retour");
        clearAllBtn = UITheme.dangerButton("Vider");
        checkoutBtn = UITheme.primaryButton("Valider la commande");

        btnRow.add(backBtn);
        btnRow.add(clearAllBtn);
        btnRow.add(checkoutBtn);

        footer.add(totalLabel, BorderLayout.WEST);
        footer.add(btnRow,     BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);

        // Listeners
        clearAllBtn.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this,
                    LanguageManager.getInstance().getText("cart.clear.confirm"),
                    "Confirmation", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                String res = clientService.clearCart(session.getClientId());
                if ("CART_CLEAR_SUCCESS".equals(res)) {
                    loadCart();
                    refreshParentCartCount();
                } else {
                    JOptionPane.showMessageDialog(this, "Erreur: " + res);
                }
            }
        });

        backBtn.addActionListener(e -> { backFrame.setVisible(true); dispose(); });
        checkoutBtn.addActionListener(e -> checkout());
    }

    private void loadCart() {
        SwingUtilities.invokeLater(() -> {
            model.setRowCount(0);
            cartProducts.clear();

            String response = clientService.getCart(session.getClientId());

            if ("CART_EMPTY".equals(response)) {
                totalLabel.setText("Total : 0.00 DH");
                model.addRow(new Object[]{
                        LanguageManager.getInstance().getText("cart.empty"), "", "", "", ""});
                return;
            }

            if (response == null || response.startsWith("ERROR")) {
                JOptionPane.showMessageDialog(this, "Erreur: " + response);
                return;
            }

            String total = "0.00";
            for (String part : response.split("\\|")) {
                if (part.startsWith("Total=")) {
                    total = part.substring("Total=".length());
                } else if (part.startsWith("ProductId=")) {
                    int    productId = Integer.parseInt(extract(part, "ProductId=", ",Product="));
                    String product   = extract(part, ",Product=", ",Qty=");
                    int    qt        = Integer.parseInt(extract(part, ",Qty=", ",Subtotal="));
                    double sub       = Double.parseDouble(part.substring(part.indexOf(",Subtotal=") + 10));
                    double unitPrice = qt > 0 ? sub / qt : 0;

                    cartProducts.add(new CartProductInfo(productId, product, qt, unitPrice, sub));
                    model.addRow(new Object[]{
                            product,
                            qt,
                            String.format("%.2f DH", unitPrice),
                            String.format("%.2f DH", sub),
                            "Supprimer"
                    });
                }
            }

            totalLabel.setText("Total : " + total + " DH");
            if (model.getRowCount() == 0) {
                model.addRow(new Object[]{LanguageManager.getInstance().getText("cart.empty"), "", "", "", ""});
            }
        });
    }

    private void removeItemFromCart(int row) {
        if (row < 0 || row >= cartProducts.size()) return;
        CartProductInfo product = cartProducts.get(row);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Supprimer \"" + product.name + "\" du panier ?",
                "Confirmation", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String response = clientService.removeFromCart(session.getClientId(), product.productId);
            if ("CART_REMOVE_SUCCESS".equals(response)) {
                loadCart();
                refreshParentCartCount();
            } else {
                JOptionPane.showMessageDialog(this, "Erreur: " + response);
            }
        }
    }

    private void refreshParentCartCount() {
        if (backFrame instanceof ShopFrame sf) sf.refreshCartCount();
    }

    private String extract(String src, String start, String end) {
        int s = src.indexOf(start), e = src.indexOf(end);
        return (s == -1 || e == -1) ? "" : src.substring(s + start.length(), e);
    }

    private void checkout() {
        if (cartProducts.isEmpty()) {
            JOptionPane.showMessageDialog(this, LanguageManager.getInstance().getText("cart.empty"));
            return;
        }
        String response = clientService.checkout(session.getClientId());
        if (response == null || response.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(this, "Erreur checkout: " + response);
            return;
        }
        String[] parts = response.split(";");
        if (parts.length == 3 && "ORDER_CREATED".equals(parts[0])) {
            session.setOrderUUID(parts[1]);
            session.setLastOrderTotal(Double.parseDouble(parts[2]));
            dispose();
            new PaymentFrame(clientService, session, backFrame).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Erreur lors de la validation de la commande.");
        }
    }

    @Override
    public void refreshTexts() {
        setTitle(LanguageManager.getInstance().getText("cart.title"));
        titleLabel.setText(LanguageManager.getInstance().getText("cart.title"));
        model.setColumnIdentifiers(new Object[]{
                LanguageManager.getInstance().getText("cart.product"),
                LanguageManager.getInstance().getText("cart.quantity"),
                LanguageManager.getInstance().getText("cart.unit.price"),
                LanguageManager.getInstance().getText("cart.subtotal"),
                LanguageManager.getInstance().getText("cart.remove")
        });
        checkoutBtn.setText(LanguageManager.getInstance().getText("cart.checkout"));
        clearAllBtn.setText(LanguageManager.getInstance().getText("cart.clear"));
        backBtn.setText("← " + LanguageManager.getInstance().getText("cart.back"));
        loadCart();
        revalidate(); repaint();
    }

    // =========================================================
    // INNER CLASSES
    // =========================================================

    static class CartProductInfo {
        int productId; String name; int quantity; double unitPrice, subtotal;
        CartProductInfo(int id, String n, int q, double u, double s) {
            productId = id; name = n; quantity = q; unitPrice = u; subtotal = s;
        }
    }

    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBackground(new Color(69, 10, 10));
            setForeground(new Color(252, 165, 165));
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setBorderPainted(false);
            setBorder(new EmptyBorder(6, 12, 6, 12));
        }
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            setText(v == null ? "Supprimer" : v.toString());
            return this;
        }
    }

    class ButtonEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JButton button;
        private int selectedRow;

        public ButtonEditor() {
            button = new JButton("Supprimer");
            button.setOpaque(true);
            button.setBackground(new Color(69, 10, 10));
            button.setForeground(new Color(252, 165, 165));
            button.setFont(new Font("Segoe UI", Font.BOLD, 11));
            button.setBorderPainted(false);
            button.setBorder(new EmptyBorder(6, 12, 6, 12));
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable t, Object v,
                boolean sel, int row, int col) {
            selectedRow = row;
            button.setText(v == null ? "Supprimer" : v.toString());
            return button;
        }

        public Object getCellEditorValue() {
            SwingUtilities.invokeLater(() -> removeItemFromCart(selectedRow));
            return "Supprimer";
        }
    }
}