package ui.components;

import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoadingPanel extends JPanel {

    public LoadingPanel(String message) {
        setLayout(new GridBagLayout());
        setBackground(UITheme.APP_BG);

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(UITheme.BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(28, 40, 28, 40));

        JLabel lbl = new JLabel(message == null || message.isBlank() ? "Chargement..." : message);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        lbl.setFont(UITheme.FONT_BODY_BOLD);

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setPreferredSize(new Dimension(220, 4));
        bar.setMaximumSize(new Dimension(220, 4));
        bar.setAlignmentX(Component.CENTER_ALIGNMENT);
        bar.setBackground(UITheme.BORDER);
        bar.setForeground(UITheme.SKY);
        bar.setBorderPainted(false);

        card.add(lbl);
        card.add(Box.createVerticalStrut(16));
        card.add(bar);

        add(card);
    }
}