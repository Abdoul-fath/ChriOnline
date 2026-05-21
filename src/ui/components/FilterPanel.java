package ui.components;

import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class FilterPanel extends JPanel {

    public interface FilterListener {
        void onApply();
        void onReset();
    }

    private final JPanel filtersContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

    public FilterPanel(FilterListener listener) {
        setLayout(new BorderLayout(10, 0));
        setBackground(UITheme.CARD_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));

        filtersContainer.setOpaque(false);

        JButton applyBtn = UITheme.primaryButton("Appliquer");
        JButton resetBtn = UITheme.secondaryButton("Réinitialiser");

        applyBtn.addActionListener(e -> { if (listener != null) listener.onApply(); });
        resetBtn.addActionListener(e -> { if (listener != null) listener.onReset(); });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(applyBtn);
        actions.add(resetBtn);

        add(filtersContainer, BorderLayout.CENTER);
        add(actions,          BorderLayout.EAST);
    }

    public void addFilter(String labelText, JComponent component) {
        JLabel label = new JLabel(labelText);
        label.setForeground(UITheme.TEXT_SECONDARY);
        label.setFont(UITheme.FONT_BODY);
        if (component instanceof JComboBox<?> cb) UITheme.styleComboBox(cb);
        filtersContainer.add(label);
        filtersContainer.add(component);
    }

    public JPanel getFiltersContainer() { return filtersContainer; }
}