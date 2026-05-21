package ui.components;

import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class AppTable extends JTable {

    public AppTable(DefaultTableModel model) {
        super(model);
        initStyle();
    }

    private void initStyle() {
        setRowHeight(44);
        setFont(UITheme.FONT_BODY);
        setForeground(UITheme.TEXT_PRIMARY);
        setBackground(UITheme.TABLE_ROW_BG);
        setGridColor(UITheme.BORDER);
        setSelectionBackground(UITheme.TABLE_SELECTION_BG);
        setSelectionForeground(Color.WHITE);
        setFillsViewportHeight(true);
        setShowVerticalLines(false);
        setShowHorizontalLines(true);
        setIntercellSpacing(new Dimension(0, 1));

        JTableHeader th = getTableHeader();
        th.setBackground(UITheme.TABLE_HEADER_BG);
        th.setForeground(UITheme.TEXT_SECONDARY);
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setPreferredSize(new Dimension(0, 40));
        th.setReorderingAllowed(false);
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER));

        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0
                            ? UITheme.TABLE_ROW_BG
                            : UITheme.TABLE_ROW_ALT_BG);
                    c.setForeground(UITheme.TEXT_PRIMARY);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return c;
            }
        });
    }
}