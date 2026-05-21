package ui.admin;

import Client.ClientSocketService;
import ui.components.AppTable;
import ui.components.ConfirmDialog;
import ui.components.SearchBarPanel;
import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

public class ManageCategoriesPanel extends JPanel {

    private final ClientSocketService clientService;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Nom", "Description"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    private final AppTable table  = new AppTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);

    public ManageCategoriesPanel(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.APP_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setBackground(UITheme.CARD_BG);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(10, 14, 10, 14)));

        SearchBarPanel search = new SearchBarPanel("Recherche :", new SearchBarPanel.SearchListener() {
            public void onSearch(String kw) {
                sorter.setRowFilter(kw.isBlank() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(kw)));
            }
            public void onReset() { sorter.setRowFilter(null); }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton addBtn    = UITheme.primaryButton("Ajouter");
        JButton editBtn   = UITheme.secondaryButton("Modifier");
        JButton deleteBtn = UITheme.dangerButton("Supprimer");
        JButton refreshBtn = UITheme.secondaryButton("Actualiser");
        right.add(addBtn); right.add(editBtn); right.add(deleteBtn); right.add(refreshBtn);

        toolbar.add(search, BorderLayout.WEST);
        toolbar.add(right,  BorderLayout.EAST);
        table.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1, true));
        scroll.getViewport().setBackground(UITheme.TABLE_ROW_BG);

        add(toolbar, BorderLayout.NORTH);
        add(scroll,  BorderLayout.CENTER);

        addBtn.addActionListener(e    -> openDialog(false));
        editBtn.addActionListener(e   -> openDialog(true));
        deleteBtn.addActionListener(e -> deleteSelected());
        refreshBtn.addActionListener(e -> refreshData());
    }

    public void refreshData() {
        model.setRowCount(0);
        String r = clientService.adminGetCategories();
        if (r == null || r.startsWith("ERROR") || "NO_CATEGORIES".equals(r)) return;
        for (String row : r.split("\\|")) {
            String[] f = row.split(";");
            if (f.length >= 3) model.addRow(new Object[]{f[0], f[1], f[2]});
        }
    }

    private void openDialog(boolean editMode) {
        Integer id = null; String name = "", desc = "";
        if (editMode) {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Sélectionne une catégorie."); return; }
            int mr = table.convertRowIndexToModel(row);
            id   = Integer.parseInt(model.getValueAt(mr, 0).toString());
            name = model.getValueAt(mr, 1).toString();
            desc = model.getValueAt(mr, 2).toString();
        }
        JTextField nameField = UITheme.styledTextField(22); nameField.setText(name);
        JTextArea  descArea  = new JTextArea(desc, 4, 22); UITheme.styleTextArea(descArea);

        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.setBackground(UITheme.CARD_BG);
        panel.add(lbl("Nom")); panel.add(nameField);
        panel.add(lbl("Description")); panel.add(new JScrollPane(descArea));

        int result = JOptionPane.showConfirmDialog(this, panel,
                editMode ? "Modifier catégorie" : "Ajouter catégorie",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            String n = nameField.getText().trim(), d = descArea.getText().trim();
            if (n.isBlank()) throw new IllegalArgumentException("Nom obligatoire.");
            String resp = editMode
                    ? clientService.adminUpdateCategory(id, n, d)
                    : clientService.adminAddCategory(n, d);
            String expected = editMode ? "ADMIN_UPDATE_CATEGORY_SUCCESS" : "ADMIN_ADD_CATEGORY_SUCCESS";
            JOptionPane.showMessageDialog(this, expected.equals(resp)
                    ? (editMode ? "Catégorie modifiée." : "Catégorie ajoutée.")
                    : "Erreur : " + resp);
            refreshData();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage()); }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Sélectionne une catégorie."); return; }
        int mr = table.convertRowIndexToModel(row);
        int id = Integer.parseInt(model.getValueAt(mr, 0).toString());
        if (!ConfirmDialog.show(this, "Confirmation", "Supprimer cette catégorie ?")) return;
        String resp = clientService.adminDeleteCategory(id);
        if ("ADMIN_DELETE_CATEGORY_SUCCESS".equals(resp)) {
            JOptionPane.showMessageDialog(this, "Catégorie supprimée."); refreshData();
        } else JOptionPane.showMessageDialog(this, "Erreur : " + resp);
    }

    private JLabel lbl(String text) {
        JLabel l = new JLabel(text); l.setForeground(UITheme.TEXT_SECONDARY); l.setFont(UITheme.FONT_BODY); return l;
    }
}