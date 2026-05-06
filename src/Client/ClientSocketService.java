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

    public boolean connect() {
        try {
            if (isConnected()) {
                return true;
            }

            lastConnectionError = null;

            socket = new Socket(SERVER_HOST, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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

    public String ping() {
        return sendRequest("PING");
    }

    public String sendRequest(String request) {
        try {
            if (!isConnected()) {
                if (!connect()) {
                    if ("ERROR:TOO_MANY_CONNECTIONS".equals(lastConnectionError)) {
                        return "ERROR:TOO_MANY_CONNECTIONS";
                    }
                    return "ERROR:SERVER_UNREACHABLE";
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

    private void extractSessionTokenIfLoginSuccess(String response) {
        if (response == null || !response.startsWith("LOGIN_SUCCESS:")) {
            return;
        }

        String[] parts = response.split(":", -1);

        if (parts.length >= 4) {
            sessionToken = parts[3];
        }
    }

    public String login(String email, String password) {
        return sendRequest("LOGIN:" + safe(email) + ":" + safe(password));
    }

    public String register(String nom, String prenom, String email, String password,
                           String address, String phone, String ville) {
        return sendRequest("REGISTER:" +
                safe(nom) + ":" +
                safe(prenom) + ":" +
                safe(email) + ":" +
                safe(password) + ":" +
                safe(address) + ":" +
                safe(phone) + ":" +
                safe(ville));
    }

    public String sendOtp(String email) {
        return sendRequest("SEND_OTP:" + safe(email));
    }

    public String verifyOtp(String email, String code) {
        return sendRequest("VERIFY_OTP:" + safe(email) + ":" + safe(code));
    }

    public String getProducts() {
        return sendRequest("GET_PRODUCTS");
    }

    public String getProduct(int productId) {
        return sendRequest("GET_PRODUCT:" + productId);
    }

    public String getCategories() {
        return sendRequest("GET_CATEGORIES");
    }

    public String addToCart(int clientId, int productId, int quantity) {
        return sendRequest("CART_ADD:" + clientId + ":" + productId + ":" + quantity);
    }

    public String getCart(int clientId) {
        return sendRequest("CART_GET:" + clientId);
    }

    public String removeFromCart(int clientId, int productId) {
        return sendRequest("CART_REMOVE:" + clientId + ":" + productId);
    }

    public String removeFromCartByName(int clientId, String productName) {
        return sendRequest("CART_REMOVE_BY_NAME:" + clientId + ":" + safe(productName));
    }

    public String clearCart(int clientId) {
        return sendRequest("CART_CLEAR:" + clientId);
    }

    public String checkout(int clientId) {
        return sendRequest("CHECKOUT:" + clientId);
    }

    public String pay(String uuid, String method) {
        return sendRequest("PAYMENT:" + safe(uuid) + ":" + safe(method));
    }

    public String makePayment(String uuid, String method) {
        return pay(uuid, method);
    }

    public String getProfile(int userId) {
        return sendRequest("GET_PROFILE:" + userId);
    }

    public String updateProfile(int userId, String fullName, String email,
                                String phone, String address, String city) {
        return sendRequest("UPDATE_PROFILE:" +
                userId + ":" +
                safe(fullName) + ":" +
                safe(email) + ":" +
                safe(phone) + ":" +
                safe(address) + ":" +
                safe(city));
    }

    public String adminAddProduct(String name, String description, double price,
                                  int stock, String image, int categoryId) {
        return sendRequest("ADMIN_ADD_PRODUCT:" +
                safe(name) + ":" +
                safe(description) + ":" +
                price + ":" +
                stock + ":" +
                safe(image) + ":" +
                categoryId);
    }

    public String adminUpdateProduct(int productId, String name, String description,
                                     double price, int stock, String image, int categoryId) {
        return sendRequest("ADMIN_UPDATE_PRODUCT:" +
                productId + ":" +
                safe(name) + ":" +
                safe(description) + ":" +
                price + ":" +
                stock + ":" +
                safe(image) + ":" +
                categoryId);
    }

    public String adminDeleteProduct(int productId) {
        return sendRequest("ADMIN_DELETE_PRODUCT:" + productId);
    }

    public String adminGetCategories() {
        return sendRequest("ADMIN_GET_CATEGORIES");
    }

    public String adminAddCategory(String name, String description) {
        return sendRequest("ADMIN_ADD_CATEGORY:" + safe(name) + ":" + safe(description));
    }

    public String adminUpdateCategory(int categoryId, String name, String description) {
        return sendRequest("ADMIN_UPDATE_CATEGORY:" +
                categoryId + ":" +
                safe(name) + ":" +
                safe(description));
    }

    public String adminDeleteCategory(int categoryId) {
        return sendRequest("ADMIN_DELETE_CATEGORY:" + categoryId);
    }

    public String adminGetUsers() {
        return sendRequest("ADMIN_GET_USERS");
    }

    public String adminGetOrders() {
        return sendRequest("ADMIN_GET_ORDERS");
    }

    public String adminUpdateOrderStatus(int orderId, String status) {
        return sendRequest("ADMIN_UPDATE_ORDER_STATUS:" + orderId + ":" + safe(status));
    }

    public String adminGetDashboardSummary() {
        return sendRequest("ADMIN_GET_DASHBOARD_SUMMARY");
    }

    public String adminGetNotifications() {
        return sendRequest("ADMIN_GET_NOTIFICATIONS");
    }

    public String adminMarkNotificationRead(int notificationId) {
        return sendRequest("ADMIN_MARK_NOTIFICATION_READ:" + notificationId);
    }

    public String adminGetStockAlerts() {
        return sendRequest("ADMIN_GET_STOCK_ALERTS");
    }

    public String adminGetStockHistory() {
        return sendRequest("ADMIN_GET_STOCK_HISTORY");
    }

    public String adminAdjustStock(int productId, int quantity, String movementType,
                                   String reason, int adminUserId) {
        return sendRequest("ADMIN_ADJUST_STOCK:" +
                productId + ":" +
                quantity + ":" +
                safe(movementType) + ":" +
                safe(reason) + ":" +
                adminUserId);
    }

    public String requestAdminChallenge(String email) {
        return sendRequest("ADMIN_AUTH_REQUEST:" + safe(email));
    }

    public String verifyAdminSignature(String email, String signature, String challenge) {
        return sendRequest("ADMIN_CHALLENGE_RESPONSE:"
                + safe(email) + ":"
                + safe(signature) + ":"
                + safe(challenge));
    }

    public String getProfileByEmail(String email) {
        return sendRequest("GET_PROFILE_BY_EMAIL:" + safe(email));
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public boolean hasSessionToken() {
        return sessionToken != null && !sessionToken.isBlank();
    }

    private String safe(String value) {
        if (value == null) return "";

        return value.replace(":", "-")
                .replace(";", ",")
                .replace("|", "/")
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();
    }

    public void close() {
        try {
            connected = false;
            sessionToken = null;

            if (in != null) {
                in.close();
                in = null;
            }

            if (out != null) {
                out.close();
                out = null;
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            socket = null;

        } catch (Exception ignored) {
        }
    }
}