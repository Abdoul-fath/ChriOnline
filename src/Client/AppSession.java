package Client;

public class AppSession {

    // utilisateur connecté
    private int userId;
    private String role;
    private String fullName;

    // données commande
    private String orderUUID;
    private double lastOrderTotal;

    public AppSession() {
        this.userId = 0;
        this.role = "";
        this.fullName = "";
        this.orderUUID = null;
        this.lastOrderTotal = 0.0;
    }

    // =========================================================
    // USER SESSION
    // =========================================================

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    // compatibilité avec l'ancien code
    public int getClientId() {
        return userId;
    }

    // compatibilité avec l'ancien code
    public void setClientId(int clientId) {
        this.userId = clientId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role != null ? role : "";
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName != null ? fullName : "";
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    public boolean isClient() {
        return "client".equalsIgnoreCase(role);
    }

    public boolean isLoggedIn() {
        return userId > 0 && role != null && !role.isBlank();
    }

    // =========================================================
    // ORDER SESSION
    // =========================================================

    public String getOrderUUID() {
        return orderUUID;
    }

    public void setOrderUUID(String orderUUID) {
        this.orderUUID = orderUUID;
    }

    public double getLastOrderTotal() {
        return lastOrderTotal;
    }

    public void setLastOrderTotal(double lastOrderTotal) {
        this.lastOrderTotal = lastOrderTotal;
    }

    public void clearOrderData() {
        this.orderUUID = null;
        this.lastOrderTotal = 0.0;
    }

    // =========================================================
    // GLOBAL SESSION RESET
    // =========================================================

    public void clearSession() {
        this.userId = 0;
        this.role = "";
        this.fullName = "";
        clearOrderData();
    }
}