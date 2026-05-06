package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.YearMonth;

public class CardPaymentDialog extends JDialog {

    private JTextField cardNumberField;
    private JTextField expiryField;
    private JPasswordField cvvField;
    private JTextField cardHolderField;
    private JButton payBtn;
    private JButton cancelBtn;
    private JLabel eyeLabel;

    private boolean paymentConfirmed = false;
    private boolean cvvVisible = false;

    public CardPaymentDialog(JFrame parent, double amount) {
        super(parent, "💳 Paiement par carte bancaire", true);
        initUI(amount);
        setLocationRelativeTo(parent);
    }

    private void initUI(double amount) {
        setSize(480, 560);
        setResizable(false);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JLabel titleLabel = new JLabel("💳 Paiement sécurisé", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel amountLabel = new JLabel(String.format("Montant à payer : %.2f DH", amount), SwingConstants.CENTER);
        amountLabel.setForeground(UITheme.GOLD);
        amountLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        amountLabel.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        // Numéro de carte
        JLabel cardLabel = new JLabel("💳 Numéro de carte");
        cardLabel.setForeground(Color.WHITE);
        cardLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        gbc.gridy++;
        formPanel.add(cardLabel, gbc);

        cardNumberField = new JTextField();
        cardNumberField.setBackground(UITheme.INPUT_BG);
        cardNumberField.setForeground(Color.WHITE);
        cardNumberField.setCaretColor(Color.WHITE);
        cardNumberField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        cardNumberField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(10, 12, 10, 12)
        ));
        
        // Formatage automatique du numéro de carte (XXXX XXXX XXXX XXXX)
        ((AbstractDocument) cardNumberField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String newString = current.substring(0, offset) + text + current.substring(offset);
                newString = newString.replaceAll("\\s", "");
                
                if (newString.length() > 16) {
                    return;
                }
                
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < newString.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(newString.charAt(i));
                }
                
                fb.remove(0, fb.getDocument().getLength());
                fb.insertString(0, formatted.toString(), attr);
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String newString = current.substring(0, offset) + text + current.substring(offset + length);
                newString = newString.replaceAll("\\s", "");
                
                if (newString.length() > 16) {
                    return;
                }
                
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < newString.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(newString.charAt(i));
                }
                
                fb.replace(0, fb.getDocument().getLength(), formatted.toString(), attrs);
            }
        });
        
        cardNumberField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c)) {
                    e.consume();
                }
            }
        });
        gbc.gridy++;
        formPanel.add(cardNumberField, gbc);

        // Info panel (expiration + CVV)
        JPanel infoPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        infoPanel.setOpaque(false);

        // Expiration avec formatage MM/AA
        JPanel expiryPanel = new JPanel(new BorderLayout(5, 5));
        expiryPanel.setOpaque(false);
        JLabel expiryLabel = new JLabel("📅 Date d'expiration (MM/AA)");
        expiryLabel.setForeground(UITheme.MUTED);
        expiryLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        
        expiryField = new JTextField();
        expiryField.setBackground(UITheme.INPUT_BG);
        expiryField.setForeground(Color.WHITE);
        expiryField.setCaretColor(Color.WHITE);
        expiryField.setHorizontalAlignment(JTextField.CENTER);
        expiryField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        expiryField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(10, 8, 10, 8)
        ));
        
        // Formatage automatique MM/AA (corrigé)
        ((AbstractDocument) expiryField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String newString = current.substring(0, offset) + text + current.substring(offset);
                
                // Nettoyer les slashs existants
                String cleanString = newString.replace("/", "");
                
                if (cleanString.length() > 4) {
                    return;
                }
                
                // Construire la version formatée
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < cleanString.length(); i++) {
                    if (i == 2 && cleanString.length() > 2) {
                        formatted.append("/");
                    }
                    formatted.append(cleanString.charAt(i));
                }
                
                fb.remove(0, fb.getDocument().getLength());
                fb.insertString(0, formatted.toString(), attr);
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String newString = current.substring(0, offset) + text + current.substring(offset + length);
                
                // Nettoyer les slashs existants
                String cleanString = newString.replace("/", "");
                
                if (cleanString.length() > 4) {
                    return;
                }
                
                // Construire la version formatée
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < cleanString.length(); i++) {
                    if (i == 2 && cleanString.length() > 2) {
                        formatted.append("/");
                    }
                    formatted.append(cleanString.charAt(i));
                }
                
                fb.replace(0, fb.getDocument().getLength(), formatted.toString(), attrs);
            }
        });
        
        expiryField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c)) {
                    e.consume();
                }
            }
        });
        
        expiryPanel.add(expiryLabel, BorderLayout.NORTH);
        expiryPanel.add(expiryField, BorderLayout.CENTER);

        // CVV avec œil
        JPanel cvvPanel = new JPanel(new BorderLayout(5, 5));
        cvvPanel.setOpaque(false);
        JLabel cvvLabel = new JLabel("🔒 CVV (3 chiffres)");
        cvvLabel.setForeground(UITheme.MUTED);
        cvvLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        
        // Panel pour le champ CVV + œil
        JPanel cvvFieldPanel = new JPanel(new BorderLayout());
        cvvFieldPanel.setOpaque(false);
        cvvFieldPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(0, 0, 0, 0)
        ));
        
        cvvField = new JPasswordField();
        cvvField.setBackground(UITheme.INPUT_BG);
        cvvField.setForeground(Color.WHITE);
        cvvField.setCaretColor(Color.WHITE);
        cvvField.setHorizontalAlignment(JTextField.CENTER);
        cvvField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cvvField.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        cvvField.setEchoChar('•');
        
        // Œil pour voir/masquer
        eyeLabel = new JLabel("👁️");
        eyeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eyeLabel.setForeground(UITheme.MUTED);
        eyeLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 8));
        
        eyeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleCvvVisibility();
                cvvField.requestFocusInWindow();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                eyeLabel.setForeground(UITheme.GOLD);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (!cvvVisible) {
                    eyeLabel.setForeground(UITheme.MUTED);
                }
            }
        });
        
        cvvField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) || cvvField.getPassword().length >= 3) {
                    e.consume();
                }
            }
        });
        
        cvvFieldPanel.add(cvvField, BorderLayout.CENTER);
        cvvFieldPanel.add(eyeLabel, BorderLayout.EAST);
        
        cvvPanel.add(cvvLabel, BorderLayout.NORTH);
        cvvPanel.add(cvvFieldPanel, BorderLayout.CENTER);

        infoPanel.add(expiryPanel);
        infoPanel.add(cvvPanel);
        gbc.gridy++;
        formPanel.add(infoPanel, gbc);

        // Nom du titulaire
        JLabel holderLabel = new JLabel("👤 Titulaire de la carte");
        holderLabel.setForeground(Color.WHITE);
        holderLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        gbc.gridy++;
        formPanel.add(holderLabel, gbc);

        cardHolderField = new JTextField();
        cardHolderField.setBackground(UITheme.INPUT_BG);
        cardHolderField.setForeground(Color.WHITE);
        cardHolderField.setCaretColor(Color.WHITE);
        cardHolderField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cardHolderField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(10, 12, 10, 12)
        ));
        gbc.gridy++;
        formPanel.add(cardHolderField, gbc);

        // Security note
        JLabel securityNote = new JLabel("🔒 Paiement 100% sécurisé - Transaction simulée");
        securityNote.setForeground(UITheme.SUCCESS);
        securityNote.setFont(new Font("SansSerif", Font.PLAIN, 11));
        securityNote.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy++;
        formPanel.add(securityNote, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setOpaque(false);

        payBtn = UITheme.primaryButton("✅ Payer " + String.format("%.2f DH", amount));
        cancelBtn = UITheme.blueButton("❌ Annuler");

        buttonPanel.add(payBtn);
        buttonPanel.add(cancelBtn);

        root.add(titleLabel, BorderLayout.NORTH);
        root.add(amountLabel, BorderLayout.NORTH);
        root.add(formPanel, BorderLayout.CENTER);
        root.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(root);

        payBtn.addActionListener(e -> validateAndPay());
        cancelBtn.addActionListener(e -> {
            paymentConfirmed = false;
            dispose();
        });
    }

    private void toggleCvvVisibility() {
        cvvVisible = !cvvVisible;
        
        if (cvvVisible) {
            cvvField.setEchoChar((char) 0);
            eyeLabel.setText("🙈");
            eyeLabel.setForeground(UITheme.GOLD);
        } else {
            cvvField.setEchoChar('•');
            eyeLabel.setText("👁️");
            eyeLabel.setForeground(UITheme.MUTED);
        }
    }

    private boolean isValidDate(String expiry) {
        try {
            String[] parts = expiry.split("/");
            if (parts.length != 2) return false;
            
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);
            
            if (month < 1 || month > 12) {
                return false;
            }
            
            // Vérifier que la date existe réellement
            int fullYear = 2000 + year;
            YearMonth yearMonth = YearMonth.of(fullYear, month);
            
            // Le mois est valide (ex: 31/02 sera refusé car février n'a que 28/29 jours)
            // On vérifie juste que le mois existe
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void validateAndPay() {
        String cvv = new String(cvvField.getPassword()).trim();
        String cardNumber = cardNumberField.getText().trim().replaceAll("\\s", "");
        String expiry = expiryField.getText().trim();
        String cardHolder = cardHolderField.getText().trim();

        // Validation du numéro de carte
        if (cardNumber.isEmpty() || cardNumber.length() < 13 || cardNumber.length() > 16) {
            showError("Numéro de carte invalide (13-16 chiffres)");
            return;
        }

        // Validation du format MM/AA
        if (!expiry.matches("\\d{2}/\\d{2}")) {
            showError("Format date invalide (MM/AA)\nExemple: 12/25");
            return;
        }

        // Validation de la date réelle
        if (!isValidDate(expiry)) {
            showError("Date d'expiration invalide");
            return;
        }

        String[] expiryParts = expiry.split("/");
        int month = Integer.parseInt(expiryParts[0]);
        int year = Integer.parseInt(expiryParts[1]);

        java.time.YearMonth current = java.time.YearMonth.now();
        int currentYear = current.getYear() % 100;
        int currentMonth = current.getMonthValue();

        if (year < currentYear || (year == currentYear && month < currentMonth)) {
            showError("Carte expirée");
            return;
        }

        // Validation CVV
        if (cvv.isEmpty() || cvv.length() < 3) {
            showError("CVV invalide (3 chiffres)");
            return;
        }

        // Validation nom titulaire
        if (cardHolder.isEmpty()) {
            showError("Nom du titulaire requis");
            return;
        }

        // Simuler un délai de traitement
        payBtn.setEnabled(false);
        payBtn.setText("⏳ Traitement en cours...");

        Timer timer = new Timer(1500, e -> {
            paymentConfirmed = true;
            dispose();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur de paiement", JOptionPane.ERROR_MESSAGE);
    }

    public boolean isPaymentConfirmed() {
        return paymentConfirmed;
    }
}