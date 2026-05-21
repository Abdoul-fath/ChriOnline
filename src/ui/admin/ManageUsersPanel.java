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

public class ManageUsersPanel extends JPanel {

    private final ClientSocketService clientService;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Nom", "Prénom", "Email", "Rôle", "Statut"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    private final AppTable table         = new AppTable(model);
    private final JTextField searchField = UITheme.styledTextField(18);
    private final JComboBox<String> roleFilter = new JComboBox<>(
            new String[]{"Tous", "admin", "client"});
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);

    public ManageUsersPanel(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.APP_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        UITheme.styleComboBox(roleFilter);

        FilterPanel fp = new FilterPanel(new FilterPanel.FilterListener() {
            public void onApply() { applyFilters(); }
            public void onReset() {
                searchField.setText("");
                roleFilter.setSelectedIndex(0);
                sorter.setRowFilter(null);
            }
        });
        fp.addFilter("Recherche :", searchField);
        fp.addFilter("Rôle :",      roleFilter);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton refreshBtn = UITheme.secondaryButton("Actualiser");
        refreshBtn.addActionListener(e -> refreshData());
        right.add(refreshBtn);
        fp.add(right, BorderLayout.SOUTH);

        table.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1, true));
        scroll.getViewport().setBackground(UITheme.TABLE_ROW_BG);

        add(fp,     BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    public void refreshData() {
        model.setRowCount(0);
        String r = clientService.adminGetUsers();
        if (r == null || r.startsWith("ERROR") || "NO_USERS".equals(r)) return;
        for (String row : r.split("\\|")) {
            String[] f = row.split(";");
            if (f.length >= 6)      model.addRow(new Object[]{f[0],f[1],f[2],f[3],f[4],f[5]});
            else if (f.length >= 5) model.addRow(new Object[]{f[0],f[1],f[2],f[3],f[4],""});
        }
    }

    private void applyFilters() {
        final String kw   = searchField.getText().trim().toLowerCase();
        final String role = roleFilter.getSelectedItem() == null
                ? "tous" : roleFilter.getSelectedItem().toString().toLowerCase();

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                boolean searchOk = kw.isEmpty()
                        || entry.getStringValue(1).toLowerCase().contains(kw)
                        || entry.getStringValue(2).toLowerCase().contains(kw)
                        || entry.getStringValue(3).toLowerCase().contains(kw);
                boolean roleOk = "tous".equals(role)
                        || entry.getStringValue(4).toLowerCase().equals(role);
                return searchOk && roleOk;
            }
        });
    }
}