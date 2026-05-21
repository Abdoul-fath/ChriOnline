package ui.admin;

import Client.ClientSocketService;
import ui.components.AppTable;
import ui.components.MetricCard;
import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class AdminDashboardPanel extends JPanel {

    private final ClientSocketService clientService;

    private final MetricCard totalProductsCard = new MetricCard("Produits",       "--", "Total catalogue");
    private final MetricCard lowStockCard      = new MetricCard("Stock faible",   "--", "Produits à surveiller");
    private final MetricCard outOfStockCard    = new MetricCard("Rupture",        "--", "Produits indisponibles");
    private final MetricCard usersCard         = new MetricCard("Utilisateurs",   "--", "Comptes enregistrés");
    private final MetricCard ordersCard        = new MetricCard("Commandes",      "--", "Total commandes");
    private final MetricCard pendingCard       = new MetricCard("En attente",     "--", "Commandes pending");
    private final MetricCard todayRevenueCard  = new MetricCard("Revenus jour",   "--", "Paiements du jour");
    private final MetricCard monthRevenueCard  = new MetricCard("Revenus mois",   "--", "Paiements du mois");

    private final DefaultTableModel notificationsModel = new DefaultTableModel(
            new Object[]{"Titre", "Message", "Niveau", "Lu", "Date"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    private final DefaultTableModel stockModel = new DefaultTableModel(
            new Object[]{"Produit", "Stock", "Seuil", "Niveau", "Statut"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    public AdminDashboardPanel(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.APP_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // ── Metrics grid ──
        JPanel grid = new JPanel(new GridLayout(2, 4, 12, 12));
        grid.setOpaque(false);
        grid.add(totalProductsCard);
        grid.add(lowStockCard);
        grid.add(outOfStockCard);
        grid.add(usersCard);
        grid.add(ordersCard);
        grid.add(pendingCard);
        grid.add(todayRevenueCard);
        grid.add(monthRevenueCard);

        // ── Tables ──
        AppTable notifTable = new AppTable(notificationsModel);
        AppTable stockTable = new AppTable(stockModel);

        JScrollPane notifScroll = new JScrollPane(notifTable);
        notifScroll.setBorder(null);
        notifScroll.getViewport().setBackground(UITheme.CARD_BG);

        JScrollPane stockScroll = new JScrollPane(stockTable);
        stockScroll.setBorder(null);
        stockScroll.getViewport().setBackground(UITheme.CARD_BG);

        JPanel tables = new JPanel(new GridLayout(1, 2, 12, 0));
        tables.setOpaque(false);
        tables.add(sectionCard("Notifications récentes", notifScroll));
        tables.add(sectionCard("Alertes stock",          stockScroll));

        add(grid,   BorderLayout.NORTH);
        add(tables, BorderLayout.CENTER);
    }

    private JPanel sectionCard(String title, JComponent content) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(UITheme.CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel lbl = new JLabel(title);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        lbl.setFont(UITheme.FONT_H3);

        p.add(lbl,     BorderLayout.NORTH);
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    public void refreshData() {
        loadSummary();
        loadNotificationsPreview();
        loadStockAlertsPreview();
    }

    private void loadSummary() {
        String response = clientService.adminGetDashboardSummary();
        if (response == null || response.startsWith("ERROR") || !response.startsWith("DASHBOARD_SUMMARY:")) {
            setFallback(); return;
        }
        String[] f = response.substring("DASHBOARD_SUMMARY:".length()).split(";");
        if (f.length < 10) { setFallback(); return; }

        totalProductsCard.setValue(f[0]);
        lowStockCard.setValue(f[1]);
        outOfStockCard.setValue(f[2]);
        usersCard.setValue(f[3]);
        ordersCard.setValue(f[4]);
        pendingCard.setValue(f[5]);
        todayRevenueCard.setValue(f[7] + " DH");
        monthRevenueCard.setValue(f[8] + " DH");
    }

    private void loadNotificationsPreview() {
        notificationsModel.setRowCount(0);
        String response = clientService.adminGetNotifications();
        if (response == null || response.startsWith("ERROR") || "NO_NOTIFICATIONS".equals(response)) return;
        String[] rows = response.split("\\|");
        for (int i = 0; i < Math.min(rows.length, 6); i++) {
            String[] f = rows[i].split(";");
            if (f.length >= 9) notificationsModel.addRow(new Object[]{f[1], f[2], f[4], f[5], f[8]});
        }
    }

    private void loadStockAlertsPreview() {
        stockModel.setRowCount(0);
        String response = clientService.adminGetStockAlerts();
        if (response == null || response.startsWith("ERROR") || "NO_STOCK_ALERTS".equals(response)) return;
        String[] rows = response.split("\\|");
        for (int i = 0; i < Math.min(rows.length, 6); i++) {
            String[] f = rows[i].split(";");
            if (f.length >= 7) stockModel.addRow(new Object[]{f[1], f[2], f[3], f[4], f[5]});
        }
    }

    private void setFallback() {
        for (MetricCard c : new MetricCard[]{totalProductsCard, lowStockCard, outOfStockCard,
                usersCard, ordersCard, pendingCard, todayRevenueCard, monthRevenueCard}) {
            c.setValue("--");
        }
    }
}