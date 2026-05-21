package ui.components;

import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MetricCard extends JPanel {

    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel subtitleLabel;

    public MetricCard(String title, String value, String subtitle) {
        setLayout(new BorderLayout(0, 6));
        setBackground(UITheme.CARD_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18)
        ));

        titleLabel = new JLabel(title);
        titleLabel.setForeground(UITheme.TEXT_SECONDARY);
        titleLabel.setFont(UITheme.FONT_BODY);

        valueLabel = new JLabel(value);
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));

        subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(UITheme.TEXT_MUTED);
        subtitleLabel.setFont(UITheme.FONT_SMALL);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(valueLabel);
        center.add(Box.createVerticalStrut(3));
        center.add(subtitleLabel);

        add(titleLabel, BorderLayout.NORTH);
        add(center,     BorderLayout.CENTER);
    }

    public void setValue(String value)       { valueLabel.setText(value); }
    public void setSubtitle(String subtitle) { subtitleLabel.setText(subtitle); }
    public void setTitle(String title)       { titleLabel.setText(title); }
}