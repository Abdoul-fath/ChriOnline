package Client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientSocketService {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected = false;

    private String sessionToken;
    private String lastConnectionError;

    public ClientSocketService() {
    }

    // =========================================================
    // CONNEXION
    // =========================================================

    public boolean connect() {
        try {
            if (isConnected()) return true;

            lastConnectionError = null;
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String msg = in.readLine();

            if ("CONNECTED_TO_SERVER".equals(msg)) {
                connected = true;
                return true;
            }
            if ("ERROR:TOO_MANY_CONNECTIONS".equals(msg)) {
                connected = false;
                lastConnectionError = "ERROR:TOO_MANY_CONNECTIONS";
                close();
                return false;
            }

            connected = false;
            lastConnectionError = "ERROR:SERVER_UNREACHABLE";
            close();
            return false;

        } catch (Exception e) {
            connected = false;
            lastConnectionError = "ERROR:SERVER_UNREACHABLE";
            return false;
        }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public String getLastConnectionError() {
        return lastConnectionError;
    }

    // =========================================================
    // ENVOI DE REQUÊTES
    // =========================================================

    /**
     * Requête publique — sans token (login, register, OTP, etc.)
     */
    public String sendRequest(String request) {
        try {
            if (!isConnected()) {
                if (!connect()) {
                    return "ERROR:TOO_MANY_CONNECTIONS".equals(lastConnectionError)
                            ? "ERROR:TOO_MANY_CONNECTIONS"
                            : "ERROR:SERVER_UNREACHABLE";
                }
            }

            out.println(request);
            String response = in.readLine();

            if (response == null) {
                connected = false;
                return "ERROR:NO_RESPONSE";
            }

            extractSessionTokenIfLoginSuccess(response);
            return response;

        } catch (Exception e) {
            connected = false;
            return "ERROR:COMMUNICATION";
        }
    }

    /**
     * ⭐ Requête sécurisée — avec token de session
     * Format envoyé : "TOKEN:xxxxx:COMMANDE:...params..."
     */
    public String sendSecureRequest(String command) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return "ERROR:NO_TOKEN";
        }
        return sendRequest("TOKEN:" + sessionToken + ":" + command);
    }

    private void extractSessionTokenIfLoginSuccess(String response) {
        if (response == null) return;

        // Login client normal
        if (response.startsWith("LOGIN_SUCCESS:")) {
            String[] parts = response.split(":", -1);
            if (parts.length >= 4) sessionToken = parts[3];
            return;
        }

        // Login admin RSA
        if (response.startsWith("ADMIN_AUTH_SUCCESS:")) {
            String[] parts = response.split(":", -1);
            if (parts.length >= 4) sessionToken = parts[3];
        }
    }

    // =========================================================
    // AUTH PUBLIQUE (sans token)
    // =========================================================

    public String ping() {
        return sendRequest("PING");
    }

    public String login(String email, String password) {
        return sendRequest("LOGIN:" + safe(email) + ":" + safe(password));
    }

    public String register(String nom, String prenom, String email, String password,
                           String address, String phone, String ville) {
        return sendRequest("REGISTER:" + safe(nom) + ":" + safe(prenom) + ":"
                + safe(email) + ":" + safe(password) + ":" + safe(address) + ":"
                + safe(phone) + ":" + safe(ville));
    }

    public String sendOtp(String email) {
        return sendRequest("SEND_OTP:" + safe(email));
    }

    public String verifyOtp(String email, String code) {
        return sendRequest("VERIFY_OTP:" + safe(email) + ":" + safe(code));
    }

    public String requestAdminChallenge(String email) {
        return sendRequest("ADMIN_AUTH_REQUEST:" + safe(email));
    }

    public String verifyAdminSignature(String email, String signature, String challenge) {
        return sendRequest("ADMIN_CHALLENGE_RESPONSE:"
                + safe(email) + ":" + safe(signature) + ":" + safe(challenge));
    }

    public String getProfileByEmail(String email) {
        return sendRequest("GET_PROFILE_BY_EMAIL:" + safe(email));
    }

    public String getKeystorePassword(String email) {
        String response = sendRequest("GET_KEYSTORE_PASSWORD:" + safe(email));
        if (response != null && response.startsWith("KEYSTORE_PASSWORD:"))
            return response.substring("KEYSTORE_PASSWORD:".length());
        return null;
    }

    // ── Produits et catégories publics ──────────────────────────
    public String getProducts() {
        return sendRequest("GET_PRODUCTS");
    }

    public String getProduct(int productId) {
        return sendRequest("GET_PRODUCT:" + productId);
    }

    public String getCategories() {
        return sendRequest("GET_CATEGORIES");
    }

    // =========================================================
    // REQUÊTES SÉCURISÉES (avec token) — CLIENT
    // =========================================================

    public String getProfile(int userId) {
        return sendSecureRequest("GET_PROFILE:" + userId);
    }

    public String updateProfile(int userId, String fullName, String email,
                                String phone, String address, String city) {
        return sendSecureRequest("UPDATE_PROFILE:" + userId + ":" + safe(fullName) + ":"
                + safe(email) + ":" + safe(phone) + ":" + safe(address) + ":" + safe(city));
    }

    public String addToCart(int clientId, int productId, int quantity) {
        return sendSecureRequest("CART_ADD:" + clientId + ":" + productId + ":" + quantity);
    }

    public String getCart(int clientId) {
        return sendSecureRequest("CART_GET:" + clientId);
    }

    public String removeFromCart(int clientId, int productId) {
        return sendSecureRequest("CART_REMOVE:" + clientId + ":" + productId);
    }

    public String removeFromCartByName(int clientId, String productName) {
        return sendSecureRequest("CART_REMOVE_BY_NAME:" + clientId + ":" + safe(productName));
    }

    public String clearCart(int clientId) {
        return sendSecureRequest("CART_CLEAR:" + clientId);
    }

    public String checkout(int clientId) {
        return sendSecureRequest("CHECKOUT:" + clientId);
    }

    public String pay(String uuid, String method) {
        return sendSecureRequest("PAYMENT:" + safe(uuid) + ":" + safe(method));
    }

    public String makePayment(String uuid, String method) {
        return pay(uuid, method);
    }

    // =========================================================
    // REQUÊTES SÉCURISÉES (avec token) — ADMIN
    // =========================================================

    public String adminGetDashboardSummary() {
        return sendSecureRequest("ADMIN_GET_DASHBOARD_SUMMARY");
    }

    public String adminGetNotifications() {
        return sendSecureRequest("ADMIN_GET_NOTIFICATIONS");
    }

    public String adminMarkNotificationRead(int notificationId) {
        return sendSecureRequest("ADMIN_MARK_NOTIFICATION_READ:" + notificationId);
    }

    public String adminGetStockAlerts() {
        return sendSecureRequest("ADMIN_GET_STOCK_ALERTS");
    }

    public String adminGetStockHistory() {
        return sendSecureRequest("ADMIN_GET_STOCK_HISTORY");
    }

    public String adminAdjustStock(int productId, int quantity, String movementType,
                                   String reason, int adminUserId) {
        return sendSecureRequest("ADMIN_ADJUST_STOCK:" + productId + ":" + quantity + ":"
                + safe(movementType) + ":" + safe(reason) + ":" + adminUserId);
    }

    public String adminGetUsers() {
        return sendSecureRequest("ADMIN_GET_USERS");
    }

    public String adminGetOrders() {
        return sendSecureRequest("ADMIN_GET_ORDERS");
    }

    public String adminUpdateOrderStatus(int orderId, String status) {
        return sendSecureRequest("ADMIN_UPDATE_ORDER_STATUS:" + orderId + ":" + safe(status));
    }

    public String adminGetCategories() {
        return sendSecureRequest("ADMIN_GET_CATEGORIES");
    }

    public String adminAddCategory(String name, String description) {
        return sendSecureRequest("ADMIN_ADD_CATEGORY:" + safe(name) + ":" + safe(description));
    }

    public String adminUpdateCategory(int categoryId, String name, String description) {
        return sendSecureRequest("ADMIN_UPDATE_CATEGORY:" + categoryId + ":"
                + safe(name) + ":" + safe(description));
    }

    public String adminDeleteCategory(int categoryId) {
        return sendSecureRequest("ADMIN_DELETE_CATEGORY:" + categoryId);
    }

    public String adminAddProduct(String name, String description, double price,
                                  int stock, String image, int categoryId) {
        return sendSecureRequest("ADMIN_ADD_PRODUCT:" + safe(name) + ":" + safe(description)
                + ":" + price + ":" + stock + ":" + safe(image) + ":" + categoryId);
    }

    public String adminUpdateProduct(int productId, String name, String description,
                                     double price, int stock, String image, int categoryId) {
        return sendSecureRequest("ADMIN_UPDATE_PRODUCT:" + productId + ":" + safe(name) + ":"
                + safe(description) + ":" + price + ":" + stock + ":" + safe(image) + ":" + categoryId);
    }

    public String adminDeleteProduct(int productId) {
        return sendSecureRequest("ADMIN_DELETE_PRODUCT:" + productId);
    }

    // =========================================================
    // SESSION
    // =========================================================

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public boolean hasSessionToken() {
        return sessionToken != null && !sessionToken.isBlank();
    }

    // =========================================================
    // UTILS
    // =========================================================

    private String safe(String value) {
        if (value == null) return "";
        return value.replace(":", "-").replace(";", ",")
                .replace("|", "/").replace("\n", " ").replace("\r", " ").trim();
    }

    public void close() {
        try {
            connected = false;
            sessionToken = null;
            if (in != null)  { in.close();  in  = null; }
            if (out != null) { out.close(); out = null; }
            if (socket != null && !socket.isClosed()) socket.close();
            socket = null;
        } catch (Exception ignored) {}
    }
}