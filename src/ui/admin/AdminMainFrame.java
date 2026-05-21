package ui.admin;

import Client.AppSession;
import Client.ClientSocketService;
import ui.LoginFrame;
import ui.components.AdminSidebar;
import ui.components.AdminTopbar;
import ui.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdminMainFrame extends JFrame {

    private final ClientSocketService clientService;
    private final AppSession session;

    private final CardLayout cardLayout  = new CardLayout();
    private final JPanel contentPanel   = new JPanel(cardLayout);

    private final AdminTopbar  topbar;
    private final AdminSidebar sidebar;

    private final AdminDashboardPanel   dashboardPanel;
    private final ManageProductsPanel   productsPanel;
    private final ManageCategoriesPanel categoriesPanel;
    private final ManageOrdersPanel     ordersPanel;
    private final ManageUsersPanel      usersPanel;
    private final NotificationsPanel    notificationsPanel;
    private final StockHistoryPanel     stockHistoryPanel;
    private final AdminStatisticsPanel  statisticsPanel;

    private final Map<String, String[]> pageMeta = new LinkedHashMap<>();

    // ⭐ Timer de vérification de session
    private Timer sessionCheckTimer;

    public AdminMainFrame(ClientSocketService clientService, AppSession session) {
        this.clientService = clientService;
        this.session       = session;

        UITheme.applyGlobalOptionPaneTheme();

        pageMeta.put("dashboard",     new String[]{"Dashboard",         "Vue d'ensemble du système"});
        pageMeta.put("products",      new String[]{"Produits",          "Gestion du catalogue produits"});
        pageMeta.put("categories",    new String[]{"Catégories",        "Gestion des catégories produits"});
        pageMeta.put("orders",        new String[]{"Commandes",         "Suivi et mise à jour des commandes"});
        pageMeta.put("users",         new String[]{"Utilisateurs",      "Clients et comptes du système"});
        pageMeta.put("notifications", new String[]{"Notifications",     "Alertes système et stock faible"});
        pageMeta.put("stockHistory",  new String[]{"Historique Stock",  "Mouvements et ajustements du stock"});
        pageMeta.put("statistics",    new String[]{"Statistiques",      "Indicateurs principaux du système"});

        dashboardPanel     = new AdminDashboardPanel(clientService);
        productsPanel      = new ManageProductsPanel(clientService, session);
        categoriesPanel    = new ManageCategoriesPanel(clientService);
        ordersPanel        = new ManageOrdersPanel(clientService);
        usersPanel         = new ManageUsersPanel(clientService);
        notificationsPanel = new NotificationsPanel(clientService);
        stockHistoryPanel  = new StockHistoryPanel(clientService, session);
        statisticsPanel    = new AdminStatisticsPanel(clientService);

        sidebar = new AdminSidebar(this::navigateTo);
        topbar  = new AdminTopbar(new AdminTopbar.TopbarActionListener() {
            public void onRefresh()           { refreshCurrentPage(); refreshTopbarBadge(); }
            public void onOpenNotifications() { navigateTo("notifications"); }
            public void onLogout()            { logout(); }
        });

        initUI();
        registerPages();
        navigateTo("dashboard");
        refreshTopbarBadge();

        // ⭐ Démarrer la vérification de session toutes les 30 secondes
        startSessionCheckTimer();
    }

    private void initUI() {
        setTitle("ChriOnline — Admin");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1380, 860);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.APP_BG);

        contentPanel.setBackground(UITheme.APP_BG);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(UITheme.APP_BG);
        center.add(topbar,       BorderLayout.NORTH);
        center.add(contentPanel, BorderLayout.CENTER);

        root.add(sidebar, BorderLayout.WEST);
        root.add(center,  BorderLayout.CENTER);

        setContentPane(root);
    }

    private void registerPages() {
        contentPanel.add(dashboardPanel,     "dashboard");
        contentPanel.add(productsPanel,      "products");
        contentPanel.add(categoriesPanel,    "categories");
        contentPanel.add(ordersPanel,        "orders");
        contentPanel.add(usersPanel,         "users");
        contentPanel.add(notificationsPanel, "notifications");
        contentPanel.add(stockHistoryPanel,  "stockHistory");
        contentPanel.add(statisticsPanel,    "statistics");
    }

    private void navigateTo(String pageId) {
        cardLayout.show(contentPanel, pageId);
        sidebar.setActivePage(pageId);

        String[] meta = pageMeta.getOrDefault(pageId,
                new String[]{"Admin", "Panneau d'administration"});
        topbar.setPageInfo(meta[0], meta[1]);

        refreshCurrentPage();
        refreshTopbarBadge();
    }

    private void refreshCurrentPage() {
        if (dashboardPanel.isShowing())     dashboardPanel.refreshData();
        if (productsPanel.isShowing())      productsPanel.refreshData();
        if (categoriesPanel.isShowing())    categoriesPanel.refreshData();
        if (ordersPanel.isShowing())        ordersPanel.refreshData();
        if (usersPanel.isShowing())         usersPanel.refreshData();
        if (notificationsPanel.isShowing()) notificationsPanel.refreshData();
        if (stockHistoryPanel.isShowing())  stockHistoryPanel.refreshData();
        if (statisticsPanel.isShowing())    statisticsPanel.refreshData();
    }

    private void refreshTopbarBadge() {
        topbar.setUnreadNotificationsCount(notificationsPanel.getUnreadCount());
    }

    // =========================================================
    // ⭐ GESTION SESSION
    // =========================================================

    /**
     * Démarre un timer qui vérifie la session toutes les 30 secondes.
     * Si la session est expirée → déconnexion automatique.
     */
    private void startSessionCheckTimer() {
        // Vérifier toutes les 30 secondes (30 000 ms)
        sessionCheckTimer = new Timer(30_000, e -> checkSession());
        sessionCheckTimer.setRepeats(true);
        sessionCheckTimer.start();
        System.out.println("⏱️ Timer de session démarré (vérification toutes les 30s)");
    }

    /**
     * Vérifie si la session est toujours valide côté serveur.
     * Envoie une requête ping avec le token — si SESSION_EXPIRED → déconnexion.
     */
    private void checkSession() {
        // Exécuter en arrière-plan pour ne pas bloquer l'UI
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                // ⭐ On utilise sendSecureRequest qui ajoute le token
                // Si la session est expirée, le serveur répond ERROR:SESSION_EXPIRED
                return clientService.adminGetDashboardSummary();
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    if (response != null && (
                            response.equals("ERROR:SESSION_EXPIRED") ||
                            response.equals("ERROR:INVALID_SESSION") ||
                            response.equals("ERROR:MISSING_TOKEN"))) {

                        System.out.println("⏰ Session expirée — déconnexion automatique");
                        forceLogout();
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    /**
     * Déconnexion manuelle (bouton Déconnexion).
     */
    private void logout() {
        int c = JOptionPane.showConfirmDialog(this,
                "Voulez-vous vous déconnecter ?",
                "Déconnexion",
                JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            forceLogout();
        }
    }

    /**
     * ⭐ Déconnexion forcée (session expirée ou logout manuel).
     */
    private void forceLogout() {
        // Arrêter le timer
        if (sessionCheckTimer != null && sessionCheckTimer.isRunning()) {
            sessionCheckTimer.stop();
            System.out.println("⏱️ Timer de session arrêté");
        }

        // Fermer la connexion
        clientService.close();

        // Retour à LoginFrame sur l'EDT
        SwingUtilities.invokeLater(() -> {
            dispose();

            // ⭐ Message si session expirée automatiquement
            JOptionPane.showMessageDialog(null,
                    "⏰ Votre session a expiré.\nVeuillez vous reconnecter.",
                    "Session expirée",
                    JOptionPane.WARNING_MESSAGE);

            new LoginFrame(clientService).setVisible(true);
        });
    }
}