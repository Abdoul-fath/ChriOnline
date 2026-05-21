package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

public class CategoriesFrame extends JFrame {

    public CategoriesFrame(List<String> categories, Consumer<String> onCategorySelected) {
        setTitle("Catégories");
        setSize(320, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 12)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(13, 17, 27),
                        getWidth(), getHeight(), new Color(18, 26, 44)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBackground(UITheme.BG);
        root.setBorder(new EmptyBorder(18, 16, 16, 16));

        // ── Header ──
        JLabel title = new JLabel("Catégories");
        title.setForeground(UITheme.TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JLabel subtitle = new JLabel("Sélectionnez une catégorie");
        subtitle.setForeground(UITheme.MUTED);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(title);
        header.add(Box.createVerticalStrut(2));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(10));

        // ── List ──
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("Tous les produits");
        for (String c : categories) {
            if (!c.equalsIgnoreCase("General")
                    && !c.equalsIgnoreCase("Toutes")
                    && !c.equalsIgnoreCase("Tous")
                    && !c.isEmpty()) {
                model.addElement(getIconForCategory(c) + "  " + c);
            }
        }

        JList<String> list = new JList<>(model);
        list.setBackground(UITheme.CARD);
        list.setForeground(UITheme.TEXT);
        list.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        list.setFixedCellHeight(44);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBorder(new EmptyBorder(4, 0, 4, 0));

        // Custom renderer
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object val,
                    int idx, boolean sel, boolean focused) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(l, val, idx, sel, focused);
                lbl.setBorder(new EmptyBorder(0, 14, 0, 14));
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                if (sel) {
                    lbl.setBackground(new Color(30, 64, 120));
                    lbl.setForeground(Color.WHITE);
                } else {
                    lbl.setBackground(idx % 2 == 0 ? UITheme.CARD : UITheme.CARD_2);
                    lbl.setForeground(UITheme.TEXT);
                }
                // "Tous les produits" en bold
                if (idx == 0) lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                return lbl;
            }
        });

        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) selectCategory(list, onCategorySelected);
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1, true));
        scroll.setBackground(UITheme.CARD);
        scroll.getViewport().setBackground(UITheme.CARD);

        // ── Buttons ──
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 10, 0));
        btnRow.setOpaque(false);

        JButton chooseBtn = UITheme.primaryButton("Choisir");
        JButton closeBtn  = UITheme.blueButton("Fermer");

        chooseBtn.addActionListener(e -> selectCategory(list, onCategorySelected));
        closeBtn.addActionListener(e -> dispose());

        btnRow.add(chooseBtn);
        btnRow.add(closeBtn);

        root.add(header, BorderLayout.NORTH);
        root.add(scroll,  BorderLayout.CENTER);
        root.add(btnRow,  BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void selectCategory(JList<String> list, Consumer<String> onCategorySelected) {
        String selected = list.getSelectedValue();
        if (selected == null || onCategorySelected == null) return;

        // Enlever l'icône si présente
        String categoryName = selected.contains("  ")
                ? selected.substring(selected.indexOf("  ") + 2).trim()
                : selected.trim();

        onCategorySelected.accept(
                categoryName.equals("Tous les produits") ? "Tous" : categoryName
        );
        dispose();
    }

    private String getIconForCategory(String category) {
        String l = category.toLowerCase();
        if (l.contains("tablette") || l.contains("phone")) return "📱";
        if (l.contains("inform")   || l.contains("informatique") || l.contains("laptop") || l.contains("ordi")) return "💻";
        if (l.contains("audio"))   return "🎧";
        if (l.contains("gaming"))  return "🎮";
        if (l.contains("access"))  return "🔌";
        if (l.contains("vetement")) return "👕";
        return "📦";
    }
}