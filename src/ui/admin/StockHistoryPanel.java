package ui.admin;

import Client.AppSession;
import Client.ClientSocketService;
import ui.components.AppTable;
import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class StockHistoryPanel extends JPanel {

    private final ClientSocketService clientService;
    private final AppSession session;

    private final DefaultTableModel alertsModel = new DefaultTableModel(
            new Object[]{"ProduitID", "Produit", "Stock", "Seuil", "Niveau", "Statut", "Date"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    private final DefaultTableModel historyModel = new DefaultTableModel(
            new Object[]{"ID", "ProduitID", "Produit", "Type", "Quantité", "Avant", "Après", "Raison", "Admin", "Date"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    private final AppTable alertsTable  = new AppTable(alertsModel);
    private final AppTable historyTable = new AppTable(historyModel);

    public StockHistoryPanel(ClientSocketService clientService, AppSession session) {
        this.clientService = clientService;
        this.session       = session;
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.APP_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toolbar.setBackground(UITheme.CARD_BG);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(10, 14, 10, 14)));

        JButton adjustBtn  = UITheme.primaryButton("Ajuster stock");
        JButton refreshBtn = UITheme.secondaryButton("Actualiser");
        toolbar.add(adjustBtn); toolbar.add(refreshBtn);

        JScrollPane alertsScroll = new JScrollPane(alertsTable);
        alertsScroll.setBorder(null);
        alertsScroll.getViewport().setBackground(UITheme.TABLE_ROW_BG);

        JScrollPane historyScroll = new JScrollPane(historyTable);
        historyScroll.setBorder(null);
        historyScroll.getViewport().setBackground(UITheme.TABLE_ROW_BG);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                sectionCard("Alertes stock", alertsScroll),
                sectionCard("Historique des mouvements", historyScroll));
        split.setResizeWeight(0.35);
        split.setDividerSize(6);
        split.setBackground(UITheme.APP_BG);

        add(toolbar, BorderLayout.NORTH);
        add(split,   BorderLayout.CENTER);

        adjustBtn.addActionListener(e  -> openAdjustDialog());
        refreshBtn.addActionListener(e -> refreshData());
    }

    private JPanel sectionCard(String title, JComponent content) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(UITheme.CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)));
        JLabel lbl = new JLabel(title);
        lbl.setForeground(UITheme.TEXT_PRIMARY); lbl.setFont(UITheme.FONT_H3);
        p.add(lbl, BorderLayout.NORTH); p.add(content, BorderLayout.CENTER);
        return p;
    }

    public void refreshData() {
        // Alerts
        alertsModel.setRowCount(0);
        String r1 = clientService.adminGetStockAlerts();
        if (r1 != null && !r1.startsWith("ERROR") && !"NO_STOCK_ALERTS".equals(r1)) {
            for (String row : r1.split("\\|")) {
                String[] f = row.split(";");
                if (f.length >= 7) alertsModel.addRow(new Object[]{f[0],f[1],f[2],f[3],f[4],f[5],f[6]});
            }
        }
        // History
        historyModel.setRowCount(0);
        String r2 = clientService.adminGetStockHistory();
        if (r2 != null && !r2.startsWith("ERROR") && !"NO_STOCK_HISTORY".equals(r2)) {
            for (String row : r2.split("\\|")) {
                String[] f = row.split(";");
                if (f.length >= 10) historyModel.addRow(new Object[]{f[0],f[1],f[2],f[3],f[4],f[5],f[6],f[7],f[8],f[9]});
            }
        }
    }

    private void openAdjustDialog() {
        JTextField idField     = UITheme.styledTextField(20);
        JTextField qtyField    = UITheme.styledTextField(20);
        JComboBox<String> type = new JComboBox<>(new String[]{"ENTREE", "SORTIE", "AJUSTEMENT"});
        UITheme.styleComboBox(type);
        JTextField reasonField = UITheme.styledTextField(20);

        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.setBackground(UITheme.CARD_BG);
        panel.add(lbl("ID Produit")); panel.add(idField);
        panel.add(lbl("Quantité"));  panel.add(qtyField);
        panel.add(lbl("Type"));      panel.add(type);
        panel.add(lbl("Raison"));    panel.add(reasonField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Ajuster le stock",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            int    productId = Integer.parseInt(idField.getText().trim());
            int    qty       = Integer.parseInt(qtyField.getText().trim());
            String t         = (String) type.getSelectedItem();
            String reason    = reasonField.getText().trim().replace(":", "-");
            int    adminId   = session != null ? session.getUserId() : 0;

            String resp = clientService.adminAdjustStock(productId, qty, t, reason, adminId);
            JOptionPane.showMessageDialog(this,
                    "ADMIN_ADJUST_STOCK_SUCCESS".equals(resp) ? "Stock ajusté." : "Erreur : " + resp);
            if ("ADMIN_ADJUST_STOCK_SUCCESS".equals(resp)) refreshData();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage()); }
    }

    private JLabel lbl(String text) {
        JLabel l = new JLabel(text); l.setForeground(UITheme.TEXT_SECONDARY); l.setFont(UITheme.FONT_BODY); return l;
    }
}