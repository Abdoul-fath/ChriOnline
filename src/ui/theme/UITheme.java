package ui.theme;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class UITheme {

    private UITheme() {}

    // =========================================================
    // PALETTE — Deep Navy Dark
    // =========================================================

    public static final Color APP_BG      = new Color(8,  12,  20);
    public static final Color SIDEBAR_BG  = new Color(12, 17,  28);
    public static final Color TOPBAR_BG   = new Color(10, 15,  25);

    public static final Color CARD_BG     = new Color(18, 26,  42);
    public static final Color CARD_BG_ALT = new Color(24, 34,  54);
    public static final Color INPUT_BG    = new Color(14, 21,  36);

    public static final Color BORDER      = new Color(38, 52,  76);
    public static final Color BORDER_SOFT = new Color(28, 40,  62);

    public static final Color TEXT_PRIMARY   = new Color(228, 238, 252);
    public static final Color TEXT_SECONDARY = new Color(160, 178, 204);
    public static final Color TEXT_MUTED     = new Color(110, 130, 158);

    public static final Color SKY       = new Color(56,  189, 248);
    public static final Color SKY_HOVER = new Color(14,  165, 233);
    public static final Color SKY_DARK  = new Color(2,   132, 199);

    public static final Color SUCCESS = new Color(34,  197, 94);
    public static final Color WARNING = new Color(234, 179, 8);
    public static final Color DANGER  = new Color(220, 38,  38);
    public static final Color INFO    = new Color(56,  189, 248);

    public static final Color TABLE_HEADER_BG    = new Color(10, 16, 28);
    public static final Color TABLE_ROW_BG        = new Color(18, 26, 42);
    public static final Color TABLE_ROW_ALT_BG    = new Color(22, 32, 50);
    public static final Color TABLE_SELECTION_BG  = new Color(30, 64, 175);

    // =========================================================
    // FONTS — Segoe UI
    // =========================================================

    public static final Font FONT_H1         = new Font("Segoe UI", Font.BOLD,  24);
    public static final Font FONT_H2         = new Font("Segoe UI", Font.BOLD,  20);
    public static final Font FONT_H3         = new Font("Segoe UI", Font.BOLD,  15);
    public static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD  = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_SMALL      = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD,  11);

    // =========================================================
    // BORDERS
    // =========================================================

    public static final Insets BUTTON_INSETS = new Insets(9, 16, 9, 16);

    public static Border panelBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(14, 14, 14, 14));
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16));
    }

    public static Border inputBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12));
    }

    // =========================================================
    // PANELS
    // =========================================================

    public static JPanel createPagePanel() {
        JPanel p = new JPanel();
        p.setBackground(APP_BG);
        p.setBorder(new EmptyBorder(18, 18, 18, 18));
        return p;
    }

    public static JPanel createCardPanel() {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setBorder(cardBorder());
        return p;
    }

    // =========================================================
    // LABELS
    // =========================================================

    public static JLabel createTitleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_PRIMARY);
        l.setFont(FONT_H2);
        return l;
    }

    public static JLabel createSubtitleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_SECONDARY);
        l.setFont(FONT_SMALL);
        return l;
    }

    // =========================================================
    // BUTTONS — with hover effect
    // =========================================================

    public static JButton primaryButton(String text) {
        return makeBtn(text, SKY_DARK, Color.WHITE, SKY_HOVER);
    }

    public static JButton secondaryButton(String text) {
        return makeBtn(text, CARD_BG_ALT, TEXT_PRIMARY, new Color(34, 48, 72));
    }

    public static JButton dangerButton(String text) {
        return makeBtn(text, new Color(153, 27, 27), Color.WHITE, DANGER);
    }

    private static JButton makeBtn(String text, Color bg, Color fg, Color hover) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(FONT_BODY_BOLD);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(BUTTON_INSETS);
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
            public void mouseExited (MouseEvent e) { b.setBackground(bg);    }
        });
        return b;
    }

    // =========================================================
    // FIELDS
    // =========================================================

    public static JTextField styledTextField(int columns) {
        JTextField f = new JTextField(columns);
        styleTextField(f);
        return f;
    }

    public static void styleTextField(JTextField field) {
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(SKY);
        field.setBorder(inputBorder());
        field.setFont(FONT_BODY);
        field.setSelectedTextColor(Color.WHITE);
        field.setSelectionColor(SKY_DARK);
        field.putClientProperty("JTextField.placeholderForeground", TEXT_MUTED);
    }

    public static void styleTextArea(JTextArea area) {
        area.setBackground(INPUT_BG);
        area.setForeground(TEXT_PRIMARY);
        area.setCaretColor(SKY);
        area.setBorder(inputBorder());
        area.setFont(FONT_BODY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setSelectedTextColor(Color.WHITE);
        area.setSelectionColor(SKY_DARK);
    }

    public static <T> void styleComboBox(JComboBox<T> cb) {
        cb.setBackground(INPUT_BG);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFont(FONT_BODY);
    }

    // =========================================================
    // GLOBAL OPTION PANE THEME
    // =========================================================

    public static void applyGlobalOptionPaneTheme() {
        UIManager.put("Panel.background",              CARD_BG);
        UIManager.put("OptionPane.background",         CARD_BG);
        UIManager.put("OptionPane.messageForeground",  TEXT_PRIMARY);
        UIManager.put("Label.foreground",              TEXT_PRIMARY);
        UIManager.put("Button.background",             SKY_DARK);
        UIManager.put("Button.foreground",             Color.WHITE);
        UIManager.put("Button.font",                   FONT_BODY_BOLD);
        UIManager.put("TextField.background",          INPUT_BG);
        UIManager.put("TextField.foreground",          TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground",     SKY);
        UIManager.put("TextArea.background",           INPUT_BG);
        UIManager.put("TextArea.foreground",           TEXT_PRIMARY);
        UIManager.put("ComboBox.background",           INPUT_BG);
        UIManager.put("ComboBox.foreground",           TEXT_PRIMARY);
        UIManager.put("ScrollPane.background",         CARD_BG);
        UIManager.put("Viewport.background",           CARD_BG);
        UIManager.put("Table.background",              TABLE_ROW_BG);
        UIManager.put("Table.foreground",              TEXT_PRIMARY);
        UIManager.put("TableHeader.background",        TABLE_HEADER_BG);
        UIManager.put("TableHeader.foreground",        TEXT_SECONDARY);
        UIManager.put("SplitPane.background",          APP_BG);
        UIManager.put("SplitPaneDivider.background",   BORDER);
    }
}