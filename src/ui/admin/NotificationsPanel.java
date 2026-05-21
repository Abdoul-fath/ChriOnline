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

public class NotificationsPanel extends JPanel {

    private final ClientSocketService clientService;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Titre", "Message", "Type", "Niveau", "Lu", "EntityType", "EntityId", "Date"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    private final AppTable table  = new AppTable(model);
    private final JComboBox<String> levelFilter = new JComboBox<>(
            new String[]{"Tous", "WARNING", "CRITICAL", "INFO"});
    private final JComboBox<String> readFilter  = new JComboBox<>(
            new String[]{"Tous", "true", "false"});
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);

    public NotificationsPanel(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.APP_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        UITheme.styleComboBox(levelFilter);
        UITheme.styleComboBox(readFilter);

        FilterPanel fp = new FilterPanel(new FilterPanel.FilterListener() {
            public void onApply() { applyFilters(); }
            public void onReset() {
                levelFilter.setSelectedIndex(0);
                readFilter.setSelectedIndex(0);
                sorter.setRowFilter(null);
            }
        });
        fp.addFilter("Niveau :", levelFilter);
        fp.addFilter("Lu :",     readFilter);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton markBtn    = UITheme.primaryButton("Marquer comme lu");
        JButton refreshBtn = UITheme.secondaryButton("Actualiser");
        right.add(markBtn);
        right.add(refreshBtn);
        fp.add(right, BorderLayout.SOUTH);

        table.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1, true));
        scroll.getViewport().setBackground(UITheme.TABLE_ROW_BG);

        add(fp,     BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> refreshData());
        markBtn.addActionListener(e    -> markSelectedAsRead());
    }

    public void refreshData() {
        model.setRowCount(0);
        String r = clientService.adminGetNotifications();
        if (r == null || r.startsWith("ERROR") || "NO_NOTIFICATIONS".equals(r)) return;
        for (String row : r.split("\\|")) {
            String[] f = row.split(";", -1);
            if (f.length >= 9) model.addRow(new Object[]{f[0],f[1],f[2],f[3],f[4],f[5],f[6],f[7],f[8]});
        }
    }

    public int getUnreadCount() {
        int count = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            Object v = model.getValueAt(i, 5);
            if (v != null && "false".equalsIgnoreCase(v.toString())) count++;
        }
        return count;
    }

    private void applyFilters() {
        final String level = levelFilter.getSelectedItem() == null
                ? "Tous" : levelFilter.getSelectedItem().toString();
        final String read  = readFilter.getSelectedItem() == null
                ? "Tous" : readFilter.getSelectedItem().toString();

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                boolean lOk = "Tous".equals(level)
                        || entry.getStringValue(4).equalsIgnoreCase(level);
                boolean rOk = "Tous".equals(read)
                        || entry.getStringValue(5).equalsIgnoreCase(read);
                return lOk && rOk;
            }
        });
    }

    private void markSelectedAsRead() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Sélectionne une notification."); return; }
        int mr = table.convertRowIndexToModel(row);
        int id = Integer.parseInt(model.getValueAt(mr, 0).toString());
        String resp = clientService.adminMarkNotificationRead(id);
        JOptionPane.showMessageDialog(this,
                "ADMIN_MARK_NOTIFICATION_READ_SUCCESS".equals(resp)
                        ? "Marquée comme lue." : "Erreur : " + resp);
        if ("ADMIN_MARK_NOTIFICATION_READ_SUCCESS".equals(resp)) refreshData();
    }
}