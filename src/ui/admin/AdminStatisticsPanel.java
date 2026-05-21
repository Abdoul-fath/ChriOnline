package ui.admin;

import Client.ClientSocketService;
import ui.components.MetricCard;
import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminStatisticsPanel extends JPanel {

    private final ClientSocketService clientService;

    private final MetricCard totalProductsCard = new MetricCard("Produits",      "--", "Catalogue global");
    private final MetricCard totalOrdersCard   = new MetricCard("Commandes",     "--", "Toutes les commandes");
    private final MetricCard pendingOrdersCard = new MetricCard("En attente",    "--", "Commandes pending");
    private final MetricCard paidOrdersCard    = new MetricCard("Payées",        "--", "Commandes réglées");
    private final MetricCard todayRevenueCard  = new MetricCard("Revenus jour",  "--", "Total du jour");
    private final MetricCard monthRevenueCard  = new MetricCard("Revenus mois",  "--", "Total mensuel");

    private final JProgressBar paidRatioBar = new JProgressBar(0, 100);

    public AdminStatisticsPanel(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.APP_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel grid = new JPanel(new GridLayout(2, 3, 12, 12));
        grid.setOpaque(false);
        grid.add(totalProductsCard); grid.add(totalOrdersCard);  grid.add(pendingOrdersCard);
        grid.add(paidOrdersCard);    grid.add(todayRevenueCard); grid.add(monthRevenueCard);

        // Ratio card
        JPanel ratioCard = new JPanel(new BorderLayout(10, 10));
        ratioCard.setBackground(UITheme.CARD_BG);
        ratioCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18)));

        JLabel ratioTitle = new JLabel("Taux de commandes payées");
        ratioTitle.setForeground(UITheme.TEXT_PRIMARY);
        ratioTitle.setFont(UITheme.FONT_H3);

        paidRatioBar.setStringPainted(true);
        paidRatioBar.setForeground(UITheme.SKY_DARK);
        paidRatioBar.setBackground(UITheme.INPUT_BG);
        paidRatioBar.setBorderPainted(false);
        paidRatioBar.setPreferredSize(new Dimension(0, 18));

        JTextArea info = new JTextArea(
                "Cette section donne un résumé analytique basé sur les données serveur. " +
                "Des courbes, filtres par période, tops produits et KPIs supplémentaires pourront y être ajoutés.");
        info.setEditable(false); info.setOpaque(false);
        info.setForeground(UITheme.TEXT_SECONDARY); info.setFont(UITheme.FONT_BODY);
        info.setLineWrap(true); info.setWrapStyleWord(true);

        ratioCard.add(ratioTitle,   BorderLayout.NORTH);
        ratioCard.add(paidRatioBar, BorderLayout.CENTER);
        ratioCard.add(info,         BorderLayout.SOUTH);

        add(grid,      BorderLayout.NORTH);
        add(ratioCard, BorderLayout.CENTER);
    }

    public void refreshData() {
        String r = clientService.adminGetDashboardSummary();
        if (r == null || r.startsWith("ERROR") || !r.startsWith("DASHBOARD_SUMMARY:")) return;
        String[] f = r.substring("DASHBOARD_SUMMARY:".length()).split(";");
        if (f.length < 10) return;

        int total   = parseInt(f[4]);
        int paid    = parseInt(f[6]);
        int pending = parseInt(f[5]);

        totalProductsCard.setValue(f[0]);
        totalOrdersCard.setValue(String.valueOf(total));
        pendingOrdersCard.setValue(String.valueOf(pending));
        paidOrdersCard.setValue(String.valueOf(paid));
        todayRevenueCard.setValue(f[7] + " DH");
        monthRevenueCard.setValue(f[8] + " DH");

        int ratio = total == 0 ? 0 : (int)((paid * 100.0) / total);
        paidRatioBar.setValue(ratio);
        paidRatioBar.setString(ratio + "%");
    }

    private int    parseInt(String v)    { try { return Integer.parseInt(v); }    catch (Exception e) { return 0; } }
}