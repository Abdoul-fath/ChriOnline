package ui;

import Client.AppSession;
import Client.ClientSocketService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class OrderHistoryFrame extends LanguageAwareFrame {

    private final ClientSocketService clientService;
    private final AppSession session;
    private JTable ordersTable;
    private DefaultTableModel model;
    private JLabel titleLabel;

    public OrderHistoryFrame(ClientSocketService clientService, AppSession session) {
        this.clientService = clientService;
        this.session       = session;
        initUI();
        loadOrders();
    }

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("profile.order.history"));
        setSize(860, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 14)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(13, 17, 27),
                        getWidth(), getHeight(), new Color(18, 26, 44)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBackground(UITheme.BG);
        root.setBorder(new EmptyBorder(20, 22, 20, 22));

        // ── Header ──
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 4, 0));

        titleLabel = new JLabel(LanguageManager.getInstance().getText("profile.order.history"));
        titleLabel.setForeground(UITheme.TEXT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JLabel subtitle = new JLabel("Historique de vos achats");
        subtitle.setForeground(UITheme.MUTED);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.add(titleLabel);
        headerText.add(subtitle);
        header.add(headerText, BorderLayout.WEST);

        // ── Table ──
        model = new DefaultTableModel(new Object[]{
                LanguageManager.getInstance().getText("profile.order.date"),
                LanguageManager.getInstance().getText("profile.order.total"),
                LanguageManager.getInstance().getText("profile.order.status"),
                LanguageManager.getInstance().getText("profile.order.details")
        }, 0) {
            public boolean isCellEditable(int row, int col) { return col == 3; }
        };

        ordersTable = new JTable(model) {
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? UITheme.CARD : UITheme.CARD_2);
                    c.setForeground(UITheme.TEXT);
                }
                return c;
            }
        };
        ordersTable.setBackground(UITheme.CARD);
        ordersTable.setForeground(UITheme.TEXT);
        ordersTable.setRowHeight(46);
        ordersTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ordersTable.setSelectionBackground(new Color(37, 99, 180));
        ordersTable.setSelectionForeground(Color.WHITE);
        ordersTable.setShowVerticalLines(false);
        ordersTable.setGridColor(UITheme.BORDER);
        ordersTable.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader th = ordersTable.getTableHeader();
        th.setBackground(new Color(10, 14, 24));
        th.setForeground(UITheme.MUTED);
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setPreferredSize(new Dimension(0, 38));
        th.setReorderingAllowed(false);

        // Align total to right
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        ordersTable.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);

        // Status renderer (colored badge)
        ordersTable.getColumnModel().getColumn(2).setCellRenderer(new StatusRenderer());

        // Details button col
        String detailsKey = LanguageManager.getInstance().getText("profile.order.details");
        ordersTable.getColumn(detailsKey).setCellRenderer(new ButtonRenderer());
        ordersTable.getColumn(detailsKey).setCellEditor(new ButtonEditor());
        ordersTable.getColumn(detailsKey).setPreferredWidth(100);
        ordersTable.getColumn(detailsKey).setMaxWidth(110);

        JScrollPane scroll = new JScrollPane(ordersTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1, true));
        scroll.setBackground(UITheme.CARD);
        scroll.getViewport().setBackground(UITheme.CARD);

        // ── Footer ──
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        JButton closeBtn = UITheme.blueButton("Fermer");
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);

        root.add(header, BorderLayout.NORTH);
        root.add(scroll,  BorderLayout.CENTER);
        root.add(footer,  BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void loadOrders() {
        model.setRowCount(0);
        // Données simulées — à remplacer par un appel clientService.getOrders()
        model.addRow(new Object[]{"2026-04-22", "1 250.00 DH", "paid",      "Voir"});
        model.addRow(new Object[]{"2026-04-21", "890.00 DH",   "pending",   "Voir"});
        model.addRow(new Object[]{"2026-04-18", "2 450.00 DH", "delivered", "Voir"});
    }

    private void showOrderDetails(int row) {
        String date   = (String) model.getValueAt(row, 0);
        String total  = (String) model.getValueAt(row, 1);
        String status = (String) model.getValueAt(row, 2);

        JPanel panel = new JPanel();
        panel.setBackground(UITheme.CARD);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(8, 4, 8, 4));

        String[][] rows = {
            {"Date",   date},
            {"Total",  total},
            {"Statut", status}
        };

        for (String[] r : rows) {
            JPanel row2 = new JPanel(new BorderLayout(16, 0));
            row2.setBackground(UITheme.CARD);
            row2.setBorder(new EmptyBorder(4, 0, 4, 0));
            JLabel k = new JLabel(r[0]);
            k.setForeground(UITheme.MUTED);
            k.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            k.setPreferredSize(new Dimension(60, 0));
            JLabel v = new JLabel(r[1]);
            v.setForeground(UITheme.TEXT);
            v.setFont(new Font("Segoe UI", Font.BOLD, 13));
            row2.add(k, BorderLayout.WEST);
            row2.add(v, BorderLayout.CENTER);
            panel.add(row2);
        }

        JOptionPane.showMessageDialog(this, panel, "Détails commande", JOptionPane.PLAIN_MESSAGE);
    }

    @Override
    public void refreshTexts() {
        setTitle(LanguageManager.getInstance().getText("profile.order.history"));
        titleLabel.setText(LanguageManager.getInstance().getText("profile.order.history"));
        model.setColumnIdentifiers(new Object[]{
                LanguageManager.getInstance().getText("profile.order.date"),
                LanguageManager.getInstance().getText("profile.order.total"),
                LanguageManager.getInstance().getText("profile.order.status"),
                LanguageManager.getInstance().getText("profile.order.details")
        });
        revalidate(); repaint();
    }

    // ── Status badge renderer ──
    static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            JLabel lbl = new JLabel();
            lbl.setOpaque(true);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setBorder(new EmptyBorder(0, 8, 0, 8));

            String s = v == null ? "" : v.toString().toLowerCase();
            switch (s) {
                case "paid"      -> { lbl.setText("Payé");     lbl.setBackground(new Color(20,83,45));  lbl.setForeground(new Color(134,239,172)); }
                case "delivered" -> { lbl.setText("Livré");    lbl.setBackground(new Color(12,74,110)); lbl.setForeground(new Color(125,211,252)); }
                case "pending"   -> { lbl.setText("En attente"); lbl.setBackground(new Color(78,50,0));  lbl.setForeground(new Color(253,186,116)); }
                case "cancelled" -> { lbl.setText("Annulé");   lbl.setBackground(new Color(69,10,10));  lbl.setForeground(new Color(252,165,165)); }
                default          -> { lbl.setText(s);          lbl.setBackground(UITheme.CARD_2);       lbl.setForeground(UITheme.MUTED); }
            }

            if (sel) lbl.setBackground(new Color(37, 99, 180));
            return lbl;
        }
    }

    // ── Button renderer ──
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true); setBorderPainted(false);
            setBackground(new Color(30, 58, 110));
            setForeground(new Color(147, 197, 253));
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setBorder(new EmptyBorder(6, 12, 6, 12));
        }
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            setText(v == null ? "Voir" : v.toString()); return this;
        }
    }

    // ── Button editor ──
    class ButtonEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JButton button;
        private int selectedRow;

        public ButtonEditor() {
            button = new JButton("Voir");
            button.setOpaque(true); button.setBorderPainted(false);
            button.setBackground(new Color(30, 58, 110));
            button.setForeground(new Color(147, 197, 253));
            button.setFont(new Font("Segoe UI", Font.BOLD, 11));
            button.setBorder(new EmptyBorder(6, 12, 6, 12));
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable t, Object v,
                boolean sel, int row, int col) {
            selectedRow = row;
            button.setText(v == null ? "Voir" : v.toString());
            return button;
        }

        public Object getCellEditorValue() {
            SwingUtilities.invokeLater(() -> showOrderDetails(selectedRow));
            return "Voir";
        }
    }
}