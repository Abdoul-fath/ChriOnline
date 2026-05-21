package ui.components;

import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class SearchBarPanel extends JPanel {

    public interface SearchListener {
        void onSearch(String keyword);
        void onReset();
    }

    private final JTextField searchField;

    public SearchBarPanel(String labelText, SearchListener listener) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));
        setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setForeground(UITheme.TEXT_SECONDARY);
        label.setFont(UITheme.FONT_BODY);

        searchField = new JTextField(22);
        UITheme.styleTextField(searchField);
        searchField.putClientProperty("JTextField.placeholderText", "Rechercher...");

        JButton searchBtn = UITheme.primaryButton("Rechercher");
        JButton resetBtn  = UITheme.secondaryButton("Reset");

        searchBtn.addActionListener(e -> { if (listener != null) listener.onSearch(searchField.getText().trim()); });
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            if (listener != null) listener.onReset();
        });

        // Enter key support
        searchField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && listener != null)
                    listener.onSearch(searchField.getText().trim());
            }
        });

        add(label);
        add(searchField);
        add(searchBtn);
        add(resetBtn);
    }

    public JTextField getSearchField() { return searchField; }
    public String getValue()           { return searchField.getText().trim(); }
    public void clear()                { searchField.setText(""); }
}