package Client;

import java.util.Scanner;

public class ClientApp {

    private final ClientSocketService clientService;
    private final Scanner scanner;
    private final AppSession session;

    public ClientApp() {
        this.clientService = new ClientSocketService();
        this.scanner = new Scanner(System.in);
        this.session = new AppSession();
    }

    // =========================================================
    // CONNECTION
    // =========================================================

    public boolean connectToServer() {
        boolean connected = clientService.connect();

        if (connected) {
            System.out.println("✅ Connecté au serveur.");
            return true;
        } else {
            System.out.println("❌ Impossible de se connecter au serveur.");
            return false;
        }
    }

    // =========================================================
    // MAIN MENU
    // =========================================================

    public void showMainMenu() {
        while (true) {
            System.out.println("\n=== MENU PRINCIPAL ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("0. Quitter");
            System.out.print("Choix : ");

            String choix = scanner.nextLine().trim();

            switch (choix) {
                case "1" -> handleLogin();
                case "2" -> handleRegister();
                case "0" -> {
                    closeConnection();
                    return;
                }
                default -> System.out.println("Choix invalide !");
            }
        }
    }

    // =========================================================
    // LOGIN / REGISTER
    // =========================================================

    private void handleLogin() {
        System.out.print("Email : ");
        String email = scanner.nextLine();

        System.out.print("Mot de passe : ");
        String password = scanner.nextLine();

        String response = clientService.login(email, password);

        if (response != null && response.startsWith("LOGIN_SUCCESS:")) {
            String[] parts = response.split(":");
            if (parts.length >= 3) {
                int userId = Integer.parseInt(parts[1]);
                String role = parts[2];

                session.setUserId(userId);
                session.setRole(role);

                System.out.println("✅ Connexion réussie. Rôle = " + role);

                if (session.isAdmin()) {
                    showAdminMenu();
                } else {
                    showClientMenu();
                }
            } else {
                System.out.println("❌ Réponse login invalide.");
            }

        } else if ("ERROR:ACCOUNT_NOT_ACTIVE".equals(response)) {
            System.out.println("⚠️ Compte non activé.");
        } else {
            System.out.println("❌ Login échoué : " + response);
        }
    }

    private void handleRegister() {
        System.out.print("Nom : ");
        String nom = scanner.nextLine();

        System.out.print("Prénom : ");
        String prenom = scanner.nextLine();

        System.out.print("Email : ");
        String email = scanner.nextLine();

        System.out.print("Mot de passe : ");
        String password = scanner.nextLine();

        System.out.print("Adresse : ");
        String address = scanner.nextLine();

        System.out.print("Téléphone : ");
        String phone = scanner.nextLine();

        System.out.print("Ville : ");
        String ville = scanner.nextLine();

        String response = clientService.register(nom, prenom, email, password, address, phone, ville);
        System.out.println("Réponse serveur : " + response);
    }

    // =========================================================
    // CLIENT MENU
    // =========================================================

    private void showClientMenu() {
        while (true) {
            System.out.println("\n=== MENU CLIENT ===");
            System.out.println("1. Voir produits");
            System.out.println("2. Détail produit");
            System.out.println("3. Panier");
            System.out.println("4. Checkout");
            System.out.println("5. Paiement");
            System.out.println("6. Voir profil");
            System.out.println("0. Logout");
            System.out.print("Choix : ");

            String choix = scanner.nextLine().trim();

            switch (choix) {
                case "1" -> getProducts();
                case "2" -> getProductDetail();
                case "3" -> menuPanier();
                case "4" -> checkout();
                case "5" -> payment();
                case "6" -> getProfile();
                case "0" -> {
                    session.clearSession();
                    return;
                }
                default -> System.out.println("Choix invalide !");
            }
        }
    }

    // =========================================================
    // ADMIN MENU
    // =========================================================

    private void showAdminMenu() {
        while (true) {
            System.out.println("\n=== MENU ADMIN ===");
            System.out.println("1. Dashboard summary");
            System.out.println("2. Voir produits");
            System.out.println("3. Voir catégories");
            System.out.println("4. Voir utilisateurs");
            System.out.println("5. Voir commandes");
            System.out.println("6. Voir notifications");
            System.out.println("7. Voir alertes stock");
            System.out.println("8. Voir historique stock");
            System.out.println("0. Logout");
            System.out.print("Choix : ");

            String choix = scanner.nextLine().trim();

            switch (choix) {
                case "1" -> adminGetDashboardSummary();
                case "2" -> getProducts();
                case "3" -> adminGetCategories();
                case "4" -> adminGetUsers();
                case "5" -> adminGetOrders();
                case "6" -> adminGetNotifications();
                case "7" -> adminGetStockAlerts();
                case "8" -> adminGetStockHistory();
                case "0" -> {
                    session.clearSession();
                    return;
                }
                default -> System.out.println("Choix invalide !");
            }
        }
    }

    // =========================================================
    // PRODUCTS
    // =========================================================

    private void getProducts() {
        String response = clientService.getProducts();
        System.out.println(response);
    }

    private void getProductDetail() {
        System.out.print("ID produit : ");
        int id = Integer.parseInt(scanner.nextLine());

        String response = clientService.getProduct(id);
        System.out.println(response);
    }

    // =========================================================
    // CART
    // =========================================================

    private void menuPanier() {
        while (true) {
            System.out.println("\n=== PANIER ===");
            System.out.println("1. Ajouter");
            System.out.println("2. Supprimer");
            System.out.println("3. Voir panier");
            System.out.println("4. Vider panier");
            System.out.println("0. Retour");
            System.out.print("Choix : ");

            String choix = scanner.nextLine().trim();

            switch (choix) {
                case "1" -> addToCart();
                case "2" -> removeFromCart();
                case "3" -> getCart();
                case "4" -> clearCart();
                case "0" -> {
                    return;
                }
                default -> System.out.println("Choix invalide !");
            }
        }
    }

    private void addToCart() {
        System.out.print("ID produit : ");
        int productId = Integer.parseInt(scanner.nextLine());

        System.out.print("Quantité : ");
        int qty = Integer.parseInt(scanner.nextLine());

        String response = clientService.addToCart(session.getClientId(), productId, qty);
        System.out.println(response);
    }

    private void removeFromCart() {
        System.out.print("ID produit : ");
        int productId = Integer.parseInt(scanner.nextLine());

        String response = clientService.removeFromCart(session.getClientId(), productId);
        System.out.println(response);
    }

    private void getCart() {
        String response = clientService.getCart(session.getClientId());
        System.out.println(response);
    }

    private void clearCart() {
        String response = clientService.clearCart(session.getClientId());
        System.out.println(response);
    }

    // =========================================================
    // CHECKOUT / PAYMENT
    // =========================================================

    private void checkout() {
        String response = clientService.checkout(session.getClientId());
        System.out.println(response);

        if (response != null && response.startsWith("ORDER_CREATED;")) {
            String[] parts = response.split(";");
            if (parts.length >= 3) {
                session.setOrderUUID(parts[1]);
                session.setLastOrderTotal(parseDouble(parts[2]));
            }
        }
    }

    private void payment() {
        String uuid = session.getOrderUUID();

        if (uuid == null || uuid.isBlank()) {
            System.out.print("UUID commande : ");
            uuid = scanner.nextLine();
        } else {
            System.out.println("UUID commande détecté : " + uuid);
        }

        System.out.print("Méthode (card/especes) : ");
        String method = scanner.nextLine();

        String response = clientService.pay(uuid, method);
        System.out.println(response);
    }

    // =========================================================
    // PROFILE
    // =========================================================

    private void getProfile() {
        String response = clientService.getProfile(session.getUserId());
        System.out.println(response);
    }

    // =========================================================
    // ADMIN QUICK ACTIONS
    // =========================================================

    private void adminGetDashboardSummary() {
        String response = clientService.adminGetDashboardSummary();
        System.out.println(response);
    }

    private void adminGetCategories() {
        String response = clientService.adminGetCategories();
        System.out.println(response);
    }

    private void adminGetUsers() {
        String response = clientService.adminGetUsers();
        System.out.println(response);
    }

    private void adminGetOrders() {
        String response = clientService.adminGetOrders();
        System.out.println(response);
    }

    private void adminGetNotifications() {
        String response = clientService.adminGetNotifications();
        System.out.println(response);
    }

    private void adminGetStockAlerts() {
        String response = clientService.adminGetStockAlerts();
        System.out.println(response);
    }

    private void adminGetStockHistory() {
        String response = clientService.adminGetStockHistory();
        System.out.println(response);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void closeConnection() {
        try {
            clientService.close();
            scanner.close();
        } catch (Exception e) {
            System.out.println("Erreur fermeture.");
        }
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {
        ClientApp client = new ClientApp();

        if (client.connectToServer()) {
            client.showMainMenu();
        }
    }
}