package ui.admin;

import Client.ClientSocketService;
import ui.components.AppTable;
import ui.components.FilterPanel;
import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

public class ManageOrdersPanel extends JPanel {

    private final ClientSocketService clientService;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "UUID", "Client", "Email", "Total", "Statut", "Date"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    private final AppTable table         = new AppTable(model);
    private final JTextField searchField = UITheme.styledTextField(18);
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"Tous", "pending", "paid", "shipped", "delivered", "cancelled"});
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);

    public ManageOrdersPanel(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.APP_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        UITheme.styleComboBox(statusFilter);

        FilterPanel filterPanel = new FilterPanel(new FilterPanel.FilterListener() {
            public void onApply() { applyFilters(); }
            public void onReset() {
                searchField.setText("");
                statusFilter.setSelectedIndex(0);
                sorter.setRowFilter(null);
            }
        });
        filterPanel.addFilter("Recherche :", searchField);
        filterPanel.addFilter("Statut :",    statusFilter);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton updateBtn  = UITheme.primaryButton("Mettre à jour statut");
        JButton refreshBtn = UITheme.secondaryButton("Actualiser");
        right.add(updateBtn);
        right.add(refreshBtn);
        filterPanel.add(right, BorderLayout.SOUTH);

        table.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1, true));
        scroll.getViewport().setBackground(UITheme.TABLE_ROW_BG);

        add(filterPanel, BorderLayout.NORTH);
        add(scroll,      BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> refreshData());
        updateBtn.addActionListener(e  -> updateStatus());
    }

    public void refreshData() {
        model.setRowCount(0);
        String r = clientService.adminGetOrders();
        if (r == null || r.startsWith("ERROR") || "NO_ORDERS".equals(r)) return;
        for (String row : r.split("\\|")) {
            String[] f = row.split(";", -1);
            if (f.length >= 7) model.addRow(new Object[]{f[0],f[1],f[2],f[3],f[4],f[5],f[6]});
        }
    }

    private void applyFilters() {
        final String kw     = searchField.getText().trim().toLowerCase();
        final String status = statusFilter.getSelectedItem() == null
                ? "Tous" : statusFilter.getSelectedItem().toString();

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                boolean searchOk = kw.isEmpty()
                        || entry.getStringValue(0).toLowerCase().contains(kw)
                        || entry.getStringValue(1).toLowerCase().contains(kw)
                        || entry.getStringValue(2).toLowerCase().contains(kw)
                        || entry.getStringValue(3).toLowerCase().contains(kw);
                boolean statusOk = "Tous".equals(status)
                        || entry.getStringValue(5).equalsIgnoreCase(status);
                return searchOk && statusOk;
            }
        });
    }

    private void updateStatus() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Sélectionne une commande."); return; }
        int mr           = table.convertRowIndexToModel(row);
        int orderId      = Integer.parseInt(model.getValueAt(mr, 0).toString());
        String curStatus = model.getValueAt(mr, 5).toString();
        String[] statuses = {"pending", "paid", "shipped", "delivered", "cancelled"};

        String selected = (String) JOptionPane.showInputDialog(this,
                "Choisir le nouveau statut :", "Mise à jour statut",
                JOptionPane.PLAIN_MESSAGE, null, statuses, curStatus);
        if (selected == null) return;

        String resp = clientService.adminUpdateOrderStatus(orderId, selected);
        JOptionPane.showMessageDialog(this,
                "ADMIN_UPDATE_ORDER_STATUS_SUCCESS".equals(resp)
                        ? "Statut mis à jour." : "Erreur : " + resp);
        if ("ADMIN_UPDATE_ORDER_STATUS_SUCCESS".equals(resp)) refreshData();
    }
}