// ── StatusBadge.java ──
package ui.components;

import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StatusBadge extends JLabel {

    public StatusBadge(String text, Color bgColor, Color fgColor) {
        super(text, SwingConstants.CENTER);
        setOpaque(true);
        setBackground(bgColor);
        setForeground(fgColor);
        setFont(new Font("Segoe UI", Font.BOLD, 10));
        setBorder(new EmptyBorder(3, 10, 3, 10));
    }

    public static StatusBadge forOrderStatus(String status) {
        String s = status == null ? "" : status.trim().toLowerCase();
        return switch (s) {
            case "paid"      -> new StatusBadge("PAYÉ",      new Color(20, 83, 45),  new Color(134, 239, 172));
            case "pending"   -> new StatusBadge("EN ATTENTE",new Color(78, 50, 0),   new Color(253, 186, 116));
            case "shipped"   -> new StatusBadge("EXPÉDIÉ",   new Color(12, 50, 80),  new Color(125, 211, 252));
            case "delivered" -> new StatusBadge("LIVRÉ",     new Color(20, 83, 45),  new Color(134, 239, 172));
            case "cancelled" -> new StatusBadge("ANNULÉ",    new Color(69, 10, 10),  new Color(252, 165, 165));
            default          -> new StatusBadge(status == null ? "N/A" : status.toUpperCase(),
                                                UITheme.CARD_BG_ALT, UITheme.TEXT_PRIMARY);
        };
    }

    public static StatusBadge forLevel(String level) {
        String s = level == null ? "" : level.trim().toUpperCase();
        return switch (s) {
            case "CRITICAL" -> new StatusBadge("CRITIQUE", new Color(69, 10, 10),  new Color(252, 165, 165));
            case "WARNING"  -> new StatusBadge("ALERTE",   new Color(78, 50, 0),   new Color(253, 186, 116));
            case "INFO"     -> new StatusBadge("INFO",     new Color(12, 50, 80),  new Color(125, 211, 252));
            default         -> new StatusBadge(level == null ? "N/A" : level,
                                               UITheme.CARD_BG_ALT, UITheme.TEXT_PRIMARY);
        };
    }

    public static StatusBadge forBoolean(boolean value, String trueText, String falseText) {
        return value
                ? new StatusBadge(trueText,  new Color(20, 83, 45), new Color(134, 239, 172))
                : new StatusBadge(falseText, new Color(69, 10, 10), new Color(252, 165, 165));
    }
}