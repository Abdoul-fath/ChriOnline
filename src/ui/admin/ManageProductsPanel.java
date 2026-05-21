package ui.admin;

import Client.AppSession;
import Client.ClientSocketService;
import ui.components.AppTable;
import ui.components.ConfirmDialog;
import ui.components.SearchBarPanel;
import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public class ManageProductsPanel extends JPanel {

    private final ClientSocketService clientService;
    private final AppSession session;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Nom", "Prix", "Image", "Catégorie", "Stock"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    private final AppTable table = new AppTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);

    public ManageProductsPanel(ClientSocketService clientService, AppSession session) {
        this.clientService = clientService;
        this.session       = session;
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.APP_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel toolbar = buildToolbar();
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1, true));
        scroll.getViewport().setBackground(UITheme.TABLE_ROW_BG);

        add(toolbar, BorderLayout.NORTH);
        add(scroll,  BorderLayout.CENTER);
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setBackground(UITheme.CARD_BG);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));

        SearchBarPanel search = new SearchBarPanel("Recherche :", new SearchBarPanel.SearchListener() {
            public void onSearch(String kw) { sorter.setRowFilter(kw.isBlank() ? null :
                    RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(kw))); }
            public void onReset() { sorter.setRowFilter(null); }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton addBtn     = UITheme.primaryButton("Ajouter");
        JButton editBtn    = UITheme.secondaryButton("Modifier");
        JButton deleteBtn  = UITheme.dangerButton("Supprimer");
        JButton refreshBtn = UITheme.secondaryButton("Actualiser");

        right.add(addBtn); right.add(editBtn); right.add(deleteBtn); right.add(refreshBtn);
        toolbar.add(search, BorderLayout.WEST);
        toolbar.add(right,  BorderLayout.EAST);

        addBtn.addActionListener(e    -> openProductDialog(false));
        editBtn.addActionListener(e   -> openProductDialog(true));
        deleteBtn.addActionListener(e -> deleteSelectedProduct());
        refreshBtn.addActionListener(e -> refreshData());

        return toolbar;
    }

    public void refreshData() {
        model.setRowCount(0);
        String response = clientService.getProducts();
        if (response == null || response.startsWith("ERROR") || "NO_PRODUCTS".equals(response)) return;
        for (String row : response.split("\\|")) {
            String[] f = row.split(";");
            if (f.length >= 6) model.addRow(new Object[]{f[0],f[1],f[2],f[3],f[4],f[5]});
        }
    }

    private void openProductDialog(boolean editMode) {
        Integer productId = null;
        String name = "", desc = "", price = "", stock = "";
        String image = "image/default.jpg", categoryName = "";

        if (editMode) {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Sélectionne un produit."); return; }
            int mr = table.convertRowIndexToModel(row);
            productId    = Integer.parseInt(model.getValueAt(mr, 0).toString());
            name         = model.getValueAt(mr, 1).toString();
            price        = model.getValueAt(mr, 2).toString();
            image        = model.getValueAt(mr, 3).toString();
            categoryName = model.getValueAt(mr, 4).toString();
            stock        = model.getValueAt(mr, 5).toString();
            String r = clientService.getProduct(productId);
            if (r != null && !r.startsWith("ERROR")) {
                String[] f = r.split(";");
                if (f.length >= 7) { desc = f[3]; stock = f[4]; image = f[5]; categoryName = f[6]; }
            }
        }

        ProductFormDialog dlg = new ProductFormDialog(SwingUtilities.getWindowAncestor(this),
                editMode, productId, name, desc, price, stock, image, categoryName);
        dlg.setVisible(true);
        if (!dlg.isSubmitted()) return;

        try {
            String n   = dlg.getProductName();
            String d   = dlg.getDescription();
            double p   = Double.parseDouble(dlg.getPriceText());
            int    s   = Integer.parseInt(dlg.getStockText());
            String img = dlg.getImagePath();
            CategoryItem cat = dlg.getSelectedCategory();

            if (n.isBlank()) throw new IllegalArgumentException("Nom obligatoire.");
            if (cat == null) throw new IllegalArgumentException("Catégorie obligatoire.");
            if (img == null || img.isBlank()) img = "image/default.jpg";

            String resp = editMode
                    ? clientService.adminUpdateProduct(productId, n, d, p, s, img, cat.id)
                    : clientService.adminAddProduct(n, d, p, s, img, cat.id);

            String expected = editMode ? "ADMIN_UPDATE_PRODUCT_SUCCESS" : "ADMIN_ADD_PRODUCT_SUCCESS";
            if (expected.equals(resp))
                JOptionPane.showMessageDialog(this, editMode ? "Produit modifié." : "Produit ajouté.");
            else
                JOptionPane.showMessageDialog(this, "Erreur : " + resp);

            refreshData();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Prix ou stock invalide.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage());
        }
    }

    private void deleteSelectedProduct() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Sélectionne un produit."); return; }
        int mr = table.convertRowIndexToModel(row);
        int id = Integer.parseInt(model.getValueAt(mr, 0).toString());
        if (!ConfirmDialog.show(this, "Confirmation", "Supprimer ce produit ?")) return;
        String resp = clientService.adminDeleteProduct(id);
        if ("ADMIN_DELETE_PRODUCT_SUCCESS".equals(resp)) {
            JOptionPane.showMessageDialog(this, "Produit supprimé."); refreshData();
        } else JOptionPane.showMessageDialog(this, "Erreur : " + resp);
    }

    private String importImage() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Images", "jpg","jpeg","png","webp","gif"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        try {
            File src = fc.getSelectedFile();
            File dir = new File("image");
            if (!dir.exists()) dir.mkdirs();
            String ext = src.getName().contains(".") ? src.getName().substring(src.getName().lastIndexOf('.')) : "";
            File dst = new File(dir, "prod_" + System.currentTimeMillis() + ext);
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return "image/" + dst.getName();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erreur import : " + ex.getMessage()); return null; }
    }

    private void loadCategories(JComboBox<CategoryItem> combo, Map<String, Integer> map) {
        combo.removeAllItems(); map.clear();
        String r = clientService.adminGetCategories();
        if (r == null || r.startsWith("ERROR") || "NO_CATEGORIES".equals(r)) return;
        for (String row : r.split("\\|")) {
            String[] f = row.split(";");
            if (f.length >= 2) { int id = Integer.parseInt(f[0]); map.put(f[1], id); combo.addItem(new CategoryItem(id, f[1])); }
        }
    }

    private JPanel fieldBlock(String title, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(title); l.setForeground(UITheme.TEXT_SECONDARY); l.setFont(UITheme.FONT_BODY);
        p.add(l, BorderLayout.NORTH); p.add(c, BorderLayout.CENTER);
        return p;
    }

    static class CategoryItem {
        final int id; final String name;
        CategoryItem(int id, String name) { this.id = id; this.name = name; }
        public String toString() { return name; }
    }

    private class ProductFormDialog extends JDialog {
        private final JTextField  nameField  = UITheme.styledTextField(24);
        private final JTextArea   descArea   = new JTextArea(5, 24);
        private final JTextField  priceField = UITheme.styledTextField(24);
        private final JTextField  stockField = UITheme.styledTextField(24);
        private final JTextField  imageField = UITheme.styledTextField(24);
        private final JComboBox<CategoryItem> catBox = new JComboBox<>();
        private final Map<String, Integer> catMap    = new LinkedHashMap<>();
        private boolean submitted = false;

        ProductFormDialog(Window owner, boolean editMode, Integer productId,
                String curName, String curDesc, String curPrice, String curStock,
                String curImage, String curCatName) {
            super(owner, editMode ? "Modifier produit" : "Ajouter produit", ModalityType.APPLICATION_MODAL);
            setSize(740, 600); setLocationRelativeTo(ManageProductsPanel.this); setResizable(false);

            UITheme.styleTextArea(descArea); UITheme.styleComboBox(catBox); imageField.setEditable(false);
            loadCategories(catBox, catMap);

            nameField.setText(curName); descArea.setText(curDesc); priceField.setText(curPrice);
            stockField.setText(curStock); imageField.setText(curImage.isBlank() ? "image/default.jpg" : curImage);

            if (curCatName != null) for (int i = 0; i < catBox.getItemCount(); i++) {
                CategoryItem it = catBox.getItemAt(i);
                if (it != null && it.name.equalsIgnoreCase(curCatName)) { catBox.setSelectedIndex(i); break; }
            }

            JPanel root = new JPanel(new BorderLayout(14, 14));
            root.setBackground(UITheme.APP_BG);
            root.setBorder(new EmptyBorder(18, 18, 18, 18));

            JPanel card = new JPanel(new BorderLayout(12, 12));
            card.setBackground(UITheme.CARD_BG);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                    new EmptyBorder(18, 20, 18, 20)));

            JLabel title = new JLabel(editMode ? "Modifier produit" : "Ajouter un produit");
            title.setForeground(UITheme.TEXT_PRIMARY); title.setFont(UITheme.FONT_H2);

            JPanel form = new JPanel(new GridBagLayout());
            form.setOpaque(false);
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(8,6,8,6); g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;

            JPanel imgRow = new JPanel(new BorderLayout(8,0)); imgRow.setOpaque(false);
            JButton importBtn = UITheme.secondaryButton("Importer");
            importBtn.addActionListener(e -> { String p = importImage(); if (p != null) imageField.setText(p); });
            imgRow.add(imageField, BorderLayout.CENTER); imgRow.add(importBtn, BorderLayout.EAST);

            g.gridx=0; g.gridy=0; form.add(fieldBlock("Nom du produit", nameField), g);
            g.gridx=1; g.gridy=0; form.add(fieldBlock("Catégorie", catBox), g);
            g.gridx=0; g.gridy=1; form.add(fieldBlock("Prix (DH)", priceField), g);
            g.gridx=1; g.gridy=1; form.add(fieldBlock("Stock", stockField), g);
            g.gridx=0; g.gridy=2; g.gridwidth=2; form.add(fieldBlock("Image", imgRow), g);
            g.gridx=0; g.gridy=3; g.gridwidth=2; g.weighty=1; g.fill=GridBagConstraints.BOTH;
            form.add(fieldBlock("Description", new JScrollPane(descArea)), g);

            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            footer.setOpaque(false);
            JButton cancel = UITheme.secondaryButton("Annuler");
            JButton save   = UITheme.primaryButton(editMode ? "Enregistrer" : "Ajouter");
            cancel.addActionListener(e -> dispose());
            save.addActionListener(e -> { submitted = true; dispose(); });
            footer.add(cancel); footer.add(save);

            card.add(title, BorderLayout.NORTH); card.add(form, BorderLayout.CENTER); card.add(footer, BorderLayout.SOUTH);
            root.add(card, BorderLayout.CENTER);
            setContentPane(root);
        }

        boolean isSubmitted()        { return submitted; }
        String getProductName()      { return nameField.getText().trim(); }
        String getDescription()      { return descArea.getText().trim(); }
        String getPriceText()        { return priceField.getText().trim(); }
        String getStockText()        { return stockField.getText().trim(); }
        String getImagePath()        { return imageField.getText().trim(); }
        CategoryItem getSelectedCategory() { return (CategoryItem) catBox.getSelectedItem(); }
    }
}