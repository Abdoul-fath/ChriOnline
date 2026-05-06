package ui;

import Client.AppSession;
import Client.ClientSocketService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PaymentFrame extends LanguageAwareFrame {

    private final ClientSocketService clientService;
    private final AppSession session;
    private final JFrame backHome;

    private JLabel titleLabel;
    private JLabel orderLbl;
    private JLabel totalLbl;
    private JComboBox<String> methods;
    private JButton payBtn;
    private JButton backToCartBtn;
    private JButton homeBtn;
    private JLabel methodLabel;

    public PaymentFrame(ClientSocketService clientService, AppSession session, JFrame backHome) {
        this.clientService = clientService;
        this.session = session;
        this.backHome = backHome;
        initUI();
    }

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("payment.title"));
        setSize(600, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new GridBagLayout());

        JPanel card = UITheme.cardPanel();
        card.setPreferredSize(new Dimension(480, 420));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(24, 32, 24, 32)
        ));

        titleLabel = new JLabel("💳 " + LanguageManager.getInstance().getText("payment.title"));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));

        JLabel icon = new JLabel("💳");
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setFont(new Font("SansSerif", Font.PLAIN, 48));
        icon.setForeground(UITheme.GOLD);

        String shortUuid = session.getOrderUUID() != null && session.getOrderUUID().length() >= 8
                ? session.getOrderUUID().substring(0, 8) + "..."
                : session.getOrderUUID();

        orderLbl = new JLabel(LanguageManager.getInstance().getText("payment.order") + " #" + shortUuid);
        orderLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        orderLbl.setForeground(UITheme.MUTED);
        orderLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));

        totalLbl = new JLabel(String.format("💰 " + LanguageManager.getInstance().getText("cart.total") + ": %.2f DH", session.getLastOrderTotal()));
        totalLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        totalLbl.setForeground(UITheme.GOLD);
        totalLbl.setFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel methodPanel = new JPanel();
        methodPanel.setOpaque(false);
        methodPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        methodPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        methodLabel = new JLabel(LanguageManager.getInstance().getText("payment.method") + ":");
        methodLabel.setForeground(Color.WHITE);

        methods = new JComboBox<>(new String[]{
                "💳 " + LanguageManager.getInstance().getText("payment.card"),
                "💰 " + LanguageManager.getInstance().getText("payment.cash")
        });
        methods.setBackground(UITheme.CARD_2);
        methods.setForeground(Color.WHITE);
        methods.setFont(new Font("SansSerif", Font.PLAIN, 14));

        methodPanel.add(methodLabel);
        methodPanel.add(methods);

        payBtn = UITheme.primaryButton("💳 " + LanguageManager.getInstance().getText("payment.confirm"));
        payBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        payBtn.setMaximumSize(new Dimension(300, 45));
        payBtn.setPreferredSize(new Dimension(300, 45));

        backToCartBtn = UITheme.blueButton("← " + LanguageManager.getInstance().getText("payment.back.cart"));
        backToCartBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backToCartBtn.setMaximumSize(new Dimension(300, 40));
        backToCartBtn.setPreferredSize(new Dimension(300, 40));

        homeBtn = UITheme.goldButton("🏠 " + LanguageManager.getInstance().getText("payment.back.shop"));
        homeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        homeBtn.setMaximumSize(new Dimension(300, 40));
        homeBtn.setPreferredSize(new Dimension(300, 40));

        card.add(Box.createVerticalStrut(10));
        card.add(icon);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(orderLbl);
        card.add(Box.createVerticalStrut(8));
        card.add(totalLbl);
        card.add(Box.createVerticalStrut(20));
        card.add(methodPanel);
        card.add(Box.createVerticalStrut(20));
        card.add(payBtn);
        card.add(Box.createVerticalStrut(12));
        card.add(backToCartBtn);
        card.add(Box.createVerticalStrut(8));
        card.add(homeBtn);

        root.add(card);
        setContentPane(root);

        payBtn.addActionListener(e -> {
            String methodRaw = methods.getSelectedItem().toString();
            String method = methodRaw.contains("💳") ? "card" : "especes";

            if (method.equals("card")) {
                // Paiement par carte bancaire → ouvrir le dialogue
                CardPaymentDialog cardDialog = new CardPaymentDialog(PaymentFrame.this, session.getLastOrderTotal());
                cardDialog.setVisible(true);

                if (!cardDialog.isPaymentConfirmed()) {
                    // L'utilisateur a annulé ou mal rempli
                    return;
                }

                // Continuer avec le paiement simulé
                payBtn.setEnabled(false);
                payBtn.setText("⏳ " + LanguageManager.getInstance().getText("payment.confirm") + "...");

                Timer timer = new Timer(500, ev -> {
                    String response = clientService.pay(session.getOrderUUID(), method);

                    if (response.startsWith("PAYMENT_SUCCESS")) {
                        JOptionPane.showMessageDialog(PaymentFrame.this,
                                "✅ " + LanguageManager.getInstance().getText("payment.success") + "\n\n" +
                                        LanguageManager.getInstance().getText("payment.order") + ": " + shortUuid + "\n" +
                                        "💳 Carte bancaire\n" +
                                        LanguageManager.getInstance().getText("cart.total") + ": " + session.getLastOrderTotal() + " DH",
                                LanguageManager.getInstance().getText("payment.success"), JOptionPane.INFORMATION_MESSAGE);
                        session.clearOrderData();
                        dispose();
                        backHome.setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(PaymentFrame.this,
                                "❌ " + LanguageManager.getInstance().getText("payment.failed") + "\n\n" + response,
                                LanguageManager.getInstance().getText("payment.failed"), JOptionPane.ERROR_MESSAGE);
                        payBtn.setEnabled(true);
                        payBtn.setText("💳 " + LanguageManager.getInstance().getText("payment.confirm"));
                    }
                });
                timer.setRepeats(false);
                timer.start();

            } else {
                // Paiement en espèces
                payBtn.setEnabled(false);
                payBtn.setText("⏳ " + LanguageManager.getInstance().getText("payment.confirm") + "...");

                Timer timer = new Timer(500, ev -> {
                    String response = clientService.pay(session.getOrderUUID(), method);

                    if (response.startsWith("PAYMENT_SUCCESS")) {
                        JOptionPane.showMessageDialog(PaymentFrame.this,
                                "✅ " + LanguageManager.getInstance().getText("payment.success") + "\n\n" +
                                        LanguageManager.getInstance().getText("payment.order") + ": " + shortUuid + "\n" +
                                        "💰 " + LanguageManager.getInstance().getText("payment.cash") + "\n" +
                                        LanguageManager.getInstance().getText("cart.total") + ": " + session.getLastOrderTotal() + " DH",
                                LanguageManager.getInstance().getText("payment.success"), JOptionPane.INFORMATION_MESSAGE);
                        session.clearOrderData();
                        dispose();
                        backHome.setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(PaymentFrame.this,
                                "❌ " + LanguageManager.getInstance().getText("payment.failed") + "\n\n" + response,
                                LanguageManager.getInstance().getText("payment.failed"), JOptionPane.ERROR_MESSAGE);
                        payBtn.setEnabled(true);
                        payBtn.setText("💳 " + LanguageManager.getInstance().getText("payment.confirm"));
                    }
                });
                timer.setRepeats(false);
                timer.start();
            }
        });

        backToCartBtn.addActionListener(e -> {
            dispose();
            new CartFrame(clientService, session, backHome).setVisible(true);
        });

        homeBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    LanguageManager.getInstance().getText("payment.cancel") + " ?",
                    "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                session.clearOrderData();
                dispose();
                backHome.setVisible(true);
            }
        });
    }

    @Override
    public void refreshTexts() {
        setTitle(LanguageManager.getInstance().getText("payment.title"));
        titleLabel.setText("💳 " + LanguageManager.getInstance().getText("payment.title"));

        String shortUuid = session.getOrderUUID() != null && session.getOrderUUID().length() >= 8
                ? session.getOrderUUID().substring(0, 8) + "..."
                : session.getOrderUUID();

        orderLbl.setText(LanguageManager.getInstance().getText("payment.order") + " #" + shortUuid);
        totalLbl.setText(String.format("💰 " + LanguageManager.getInstance().getText("cart.total") + ": %.2f DH", session.getLastOrderTotal()));
        methodLabel.setText(LanguageManager.getInstance().getText("payment.method") + ":");

        methods.removeAllItems();
        methods.addItem("💳 " + LanguageManager.getInstance().getText("payment.card"));
        methods.addItem("💰 " + LanguageManager.getInstance().getText("payment.cash"));

        payBtn.setText("💳 " + LanguageManager.getInstance().getText("payment.confirm"));
        backToCartBtn.setText("← " + LanguageManager.getInstance().getText("payment.back.cart"));
        homeBtn.setText("🏠 " + LanguageManager.getInstance().getText("payment.back.shop"));

        revalidate();
        repaint();
    }
}