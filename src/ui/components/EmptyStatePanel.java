package ui.components;

import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class EmptyStatePanel extends JPanel {

    public EmptyStatePanel(String title, String message) {
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
        card.setBorder(new EmptyBorder(32, 40, 32, 40));

        JLabel ico = new JLabel("📭");
        ico.setFont(new Font("SansSerif", Font.PLAIN, 38));
        ico.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLbl = new JLabel(title == null || title.isBlank() ? "Aucune donnée" : title);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        titleLbl.setFont(UITheme.FONT_H3);

        JLabel msgLbl = new JLabel("<html><div style='text-align:center;width:240px;'>" +
                (message == null ? "" : message) + "</div></html>");
        msgLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        msgLbl.setForeground(UITheme.TEXT_SECONDARY);
        msgLbl.setFont(UITheme.FONT_BODY);

        card.add(ico);
        card.add(Box.createVerticalStrut(12));
        card.add(titleLbl);
        card.add(Box.createVerticalStrut(8));
        card.add(msgLbl);

        add(card);
    }
}