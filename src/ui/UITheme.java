package ui;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class UITheme {

    // =========================================================
    // PALETTE PRINCIPALE — Dark Blue-Gray moderne
    // =========================================================

    public static final Color BG       = new Color(13, 17, 27);
    public static final Color CARD     = new Color(20, 27, 43);
    public static final Color CARD_2   = new Color(26, 35, 54);
    public static final Color INPUT_BG = new Color(16, 22, 36);
    public static final Color BORDER   = new Color(42, 56, 80);

    public static final Color TEXT     = new Color(228, 237, 252);
    public static final Color MUTED    = new Color(148, 165, 190);

    public static final Color GREEN    = new Color(46, 204, 113);
    public static final Color BLUE     = new Color(59, 130, 246);
    public static final Color RED      = new Color(239, 68, 68);
    public static final Color GOLD     = new Color(251, 191, 36);
    public static final Color WARNING  = new Color(245, 158, 11);
    public static final Color SUCCESS  = new Color(46, 204, 113);
    public static final Color SKY      = new Color(56, 189, 248);

    // Hover variants
    private static final Color GREEN_HOVER = new Color(74, 222, 128);
    private static final Color BLUE_HOVER  = new Color(96, 165, 250);
    private static final Color RED_HOVER   = new Color(252, 100, 100);
    private static final Color GOLD_HOVER  = new Color(252, 211, 77);

    // =========================================================
    // SETUP FLATLAF
    // =========================================================

    public static void setupLookAndFeel() {
        try {
            FlatDarkLaf.setup();

            UIManager.put("defaultFont",              new Font("Segoe UI", Font.PLAIN, 13));
            UIManager.put("Button.arc",               10);
            UIManager.put("Component.arc",            8);
            UIManager.put("TextComponent.arc",        8);
            UIManager.put("ScrollBar.thumbArc",       999);
            UIManager.put("ScrollBar.width",          8);
            UIManager.put("ScrollBar.thumb",          BORDER);
            UIManager.put("ScrollBar.track",          BG);
            UIManager.put("TabbedPane.selectedBackground", CARD_2);
            UIManager.put("Table.rowHeight",          42);
            UIManager.put("Table.intercellSpacing",   new Dimension(0, 1));
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines",  false);
            UIManager.put("Table.gridColor",          BORDER);
            UIManager.put("SplitPane.dividerSize",    1);
            UIManager.put("PopupMenu.background",     CARD);
            UIManager.put("MenuItem.background",      CARD);
            UIManager.put("MenuItem.foreground",      TEXT);
            UIManager.put("MenuItem.selectionBackground", BLUE);
            UIManager.put("MenuItem.selectionForeground", Color.WHITE);

            // OptionPane
            UIManager.put("OptionPane.background",         CARD);
            UIManager.put("Panel.background",              CARD);
            UIManager.put("OptionPane.messageForeground",  TEXT);
            UIManager.put("Button.background",             BLUE);
            UIManager.put("Button.foreground",             Color.WHITE);

        } catch (Exception e) {
            System.err.println("FlatLaf non disponible, fallback Swing natif.");
        }
    }

    // =========================================================
    // IMAGE CACHE
    // =========================================================

    private static final String IMAGES_DIR = "image";
    private static final Map<String, ImageIcon> imageCache = new HashMap<>();

    private UITheme() {}

    // =========================================================
    // BORDERS
    // =========================================================

    public static TitledBorder titledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), title);
        border.setTitleColor(MUTED);
        border.setTitleFont(new Font("Segoe UI", Font.PLAIN, 11));
        return border;
    }

    public static TitledBorder titledBorder(String title, Font font) {
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), title);
        border.setTitleColor(MUTED);
        border.setTitleFont(font);
        return border;
    }

    public static Border roundedInputBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(9, 13, 9, 13)
        );
    }

    // =========================================================
    // IMAGES
    // =========================================================

    private static ImageIcon createPlaceholderIcon(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(CARD_2);
        g2d.fillRoundRect(0, 0, width, height, 10, 10);
        g2d.setColor(MUTED);
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "No Image";
        g2d.drawString(text, (width - fm.stringWidth(text)) / 2, (height + fm.getAscent()) / 2);
        g2d.dispose();
        return new ImageIcon(img);
    }

    public static ImageIcon loadProductImage(String path, int width, int height) {
        if (path == null || path.trim().isEmpty()) return getScaledPlaceholder(width, height);
        String normalizedPath = path.trim().replace("\\", "/");
        String cacheKey = normalizedPath + "_" + width + "x" + height;
        if (imageCache.containsKey(cacheKey)) return imageCache.get(cacheKey);
        try {
            File imageFile = resolveImageFile(normalizedPath);
            if (imageFile == null || !imageFile.exists()) {
                ImageIcon ph = getScaledPlaceholder(width, height);
                imageCache.put(cacheKey, ph);
                return ph;
            }
            ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
            if (icon.getIconWidth() <= 0) {
                ImageIcon ph = getScaledPlaceholder(width, height);
                imageCache.put(cacheKey, ph);
                return ph;
            }
            Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            ImageIcon result = new ImageIcon(scaled);
            imageCache.put(cacheKey, result);
            return result;
        } catch (Exception e) {
            ImageIcon ph = getScaledPlaceholder(width, height);
            imageCache.put(cacheKey, ph);
            return ph;
        }
    }

    private static File resolveImageFile(String path) {
        File f = new File(path);
        if (f.exists()) return f;
        File f2 = new File(IMAGES_DIR, new File(path).getName());
        return f2.exists() ? f2 : null;
    }

    private static ImageIcon getScaledPlaceholder(int w, int h) {
        String key = "placeholder_" + w + "x" + h;
        if (!imageCache.containsKey(key)) imageCache.put(key, createPlaceholderIcon(w, h));
        return imageCache.get(key);
    }

    public static void clearCache() { imageCache.clear(); }

    // =========================================================
    // PASSWORD FIELD WITH EYE
    // =========================================================

    public static JPanel createPasswordFieldWithEye(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setBackground(INPUT_BG);
        passwordField.setForeground(TEXT);
        passwordField.setCaretColor(SKY);
        passwordField.setBorder(new EmptyBorder(9, 13, 9, 13));
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        passwordField.setEchoChar('•');

        JLabel eyeLabel = new JLabel("👁");
        eyeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eyeLabel.setForeground(MUTED);
        eyeLabel.setBorder(new EmptyBorder(0, 6, 0, 10));
        eyeLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        final boolean[] visible = {false};
        eyeLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                visible[0] = !visible[0];
                passwordField.setEchoChar(visible[0] ? (char) 0 : '•');
                eyeLabel.setText(visible[0] ? "🙈" : "👁");
                eyeLabel.setForeground(visible[0] ? GOLD : MUTED);
            }
            public void mouseEntered(MouseEvent e) { eyeLabel.setForeground(GOLD); }
            public void mouseExited(MouseEvent e) { if (!visible[0]) eyeLabel.setForeground(MUTED); }
        });

        JPanel fieldPanel = new JPanel(new BorderLayout());
        fieldPanel.setBackground(INPUT_BG);
        fieldPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(0, 0, 0, 0)
        ));

        JPanel topLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 13, 4));
        topLabel.setOpaque(false);
        JLabel lbl = new JLabel(title);
        lbl.setForeground(MUTED);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        topLabel.add(lbl);

        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(INPUT_BG);
        outer.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        outer.add(topLabel, BorderLayout.NORTH);
        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(INPUT_BG);
        inner.add(passwordField, BorderLayout.CENTER);
        inner.add(eyeLabel, BorderLayout.EAST);
        outer.add(inner, BorderLayout.CENTER);

        panel.add(outer, BorderLayout.CENTER);
        panel.putClientProperty("passwordField", passwordField);
        return panel;
    }

    public static JPasswordField getPasswordFieldFromPanel(JPanel panel) {
        Object f = panel.getClientProperty("passwordField");
        return (f instanceof JPasswordField) ? (JPasswordField) f : null;
    }

    // =========================================================
    // FONTS
    // =========================================================

    public static Font titleFont()    { return new Font("Segoe UI", Font.BOLD, 26); }
    public static Font subtitleFont() { return new Font("Segoe UI", Font.BOLD, 16); }
    public static Font normalFont()   { return new Font("Segoe UI", Font.PLAIN, 13); }
    public static Font smallFont()    { return new Font("Segoe UI", Font.PLAIN, 11); }
    public static Font priceFont()    { return new Font("Segoe UI", Font.BOLD, 16); }

    // =========================================================
    // HOVER HELPER
    // =========================================================

    public static void addHoverEffect(JButton button, Color hoverColor) {
        Color orig = button.getBackground();
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { button.setBackground(hoverColor); }
            public void mouseExited(MouseEvent e)  { button.setBackground(orig); }
        });
    }

    // =========================================================
    // BUTTONS
    // =========================================================

    private static JButton baseButton(String text, Color bg, Color fg, Color hover) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(10, 20, 10, 20));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addHoverEffect(b, hover);
        return b;
    }

    public static JButton primaryButton(String text) {
        return baseButton(text, GREEN, Color.WHITE, GREEN_HOVER);
    }

    public static JButton blueButton(String text) {
        return baseButton(text, BLUE, Color.WHITE, BLUE_HOVER);
    }

    public static JButton dangerButton(String text) {
        return baseButton(text, RED, Color.WHITE, RED_HOVER);
    }

    public static JButton goldButton(String text) {
        return baseButton(text, GOLD, new Color(13, 17, 27), GOLD_HOVER);
    }

    public static JButton iconButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setBorder(new EmptyBorder(5, 10, 5, 10));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addHoverEffect(btn, bg.brighter());
        return btn;
    }

    // =========================================================
    // FIELDS
    // =========================================================

    public static JTextField textField() {
        JTextField field = new JTextField();
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT);
        field.setCaretColor(SKY);
        field.setBorder(roundedInputBorder());
        field.setFont(normalFont());
        field.setSelectedTextColor(Color.WHITE);
        field.setSelectionColor(BLUE);
        return field;
    }

    public static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT);
        field.setCaretColor(SKY);
        field.setBorder(roundedInputBorder());
        field.setFont(normalFont());
        field.setEchoChar('•');
        return field;
    }

    // =========================================================
    // PANELS
    // =========================================================

    public static JPanel darkPanel() {
        JPanel p = new JPanel();
        p.setBackground(BG);
        return p;
    }

    public static JPanel cardPanel() {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(14, 14, 14, 14)
        ));
        return p;
    }

    // =========================================================
    // LABELS
    // =========================================================

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(normalFont());
        return l;
    }

    public static JLabel mutedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(smallFont());
        return l;
    }

    public static JLabel titleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(titleFont());
        return l;
    }

    public static JLabel priceLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(GOLD);
        l.setFont(priceFont());
        return l;
    }
}