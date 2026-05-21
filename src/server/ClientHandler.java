package server;

import dao.CategoryDAO;
import dao.OrderDAO;
import dao.UserCertificateDAO;
import dao.UserDAO;
import model.Cart;
import model.CartItem;
import model.Category;
import model.DashboardSummary;
import model.Notification;
import model.Order;
import model.Payment;
import model.Product;
import model.StockAlert;
import model.StockMovement;
import model.User;
import model.UserCertificateInfo;
import security.LoginAttemptLimiter;
import security.NonceManager;
import security.SecureSessionManager;
import service.AuthService;
import service.CartService;
import service.DashboardService;
import service.NotificationService;
import service.OrderService;
import service.OtpService;
import service.PaymentService;
import service.ProductService;
import service.StockService;
import util.AppLogger;
import security.CommandValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler extends Thread {

    private static final LoginAttemptLimiter  loginLimiter   = new LoginAttemptLimiter();
    private static final SecureSessionManager sessionManager = new SecureSessionManager();
    private static final NonceManager         nonceManager   = new NonceManager();

    private final Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    private final CartService         cartService;
    private final ProductService      productService;
    private       OrderService        orderService;
    private final PaymentService      paymentService;
    private final AuthService         authService;
    private final OtpService          otpService;
    private final NotificationService notificationService;
    private final StockService        stockService;
    private final DashboardService    dashboardService;

    public ClientHandler(Socket socket) {
        this.clientSocket        = socket;
        this.cartService         = new CartService();
        this.productService      = new ProductService();
        this.paymentService      = new PaymentService();
        this.authService         = new AuthService();
        this.otpService          = new OtpService();
        this.notificationService = new NotificationService();
        this.stockService        = new StockService();
        this.dashboardService    = new DashboardService();

        try {
            this.orderService = new OrderService();
        } catch (Exception e) {
            e.printStackTrace();
            this.orderService = null;
        }

        try {
            this.in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Erreur initialisation ClientHandler : " + e.getMessage());
        }
    }

    @Override
    public void run() {
        String ip = clientSocket.getInetAddress().getHostAddress();
        try {
            out.println("CONNECTED_TO_SERVER");
            AppLogger.SERVER.info("Client connecté : {}", ip);

            String request;
            while ((request = in.readLine()) != null) {
                AppLogger.SERVER.info("Requête reçue depuis {} : {}", ip, request);
                String response = handleRequest(request);
                if (response != null) out.println(response);
            }
        } catch (IOException e) {
            AppLogger.SERVER.warn("Client déconnecté : {}", ip);
        } finally {
            closeResources();
        }
    }

    private String handleRequest(String rawRequest) {
        try {
            if (rawRequest == null || rawRequest.trim().isEmpty()) return "ERROR:EMPTY_REQUEST";

            if (!CommandValidator.isValidRequest(rawRequest)) {
                AppLogger.SECURITY.warn("Commande invalide bloquée depuis {} : {}",
                        clientSocket.getInetAddress().getHostAddress(), rawRequest);
                return "ERROR:INVALID_COMMAND";
            }

            if (rawRequest.equalsIgnoreCase("PING")) return "PONG";

            // ── Commandes publiques (sans token) ────────────────────────
            if (rawRequest.startsWith("LOGIN:"))                    return handleLogin(rawRequest);
            if (rawRequest.startsWith("REGISTER:"))                 return handleRegister(rawRequest);
            if (rawRequest.startsWith("SEND_OTP:"))                 return handleSendOtp(rawRequest);
            if (rawRequest.startsWith("VERIFY_OTP:"))               return handleVerifyOtp(rawRequest);
            if (rawRequest.startsWith("ADMIN_AUTH_REQUEST:"))       return handleAdminAuthRequest(rawRequest);
            if (rawRequest.startsWith("ADMIN_CHALLENGE_RESPONSE:")) return handleAdminChallengeResponse(rawRequest);
            if (rawRequest.startsWith("GET_PROFILE_BY_EMAIL:"))     return handleGetProfileByEmail(rawRequest);
            if (rawRequest.startsWith("GET_KEYSTORE_PASSWORD:"))    return handleGetKeystorePassword(rawRequest);
            if (rawRequest.equalsIgnoreCase("GET_CATEGORIES"))      return handleGetCategories();
            if (rawRequest.equalsIgnoreCase("GET_PRODUCTS"))        return handleGetProducts();
            if (rawRequest.startsWith("GET_PRODUCT:"))              return handleGetProduct(rawRequest);
            if (rawRequest.equalsIgnoreCase("ADMIN_GET_NONCE"))     return handleAdminGetNonce();

            // ── Toutes les autres commandes nécessitent un TOKEN ─────────
            // Format : "TOKEN:xxxxxx:COMMANDE:...params..."
            if (!rawRequest.startsWith("TOKEN:")) {
                AppLogger.SECURITY.warn("Token manquant depuis IP {}",
                        clientSocket.getInetAddress().getHostAddress());
                return "ERROR:MISSING_TOKEN";
            }

            // ── Extraire token et commande ───────────────────────────────
            // "TOKEN:" = 6 chars
            int tokenEnd = rawRequest.indexOf(":", 6);
            if (tokenEnd < 0) return "ERROR:MALFORMED_REQUEST";

            String token   = rawRequest.substring(6, tokenEnd);       // le token
            String command = rawRequest.substring(tokenEnd + 1);      // ⭐ la vraie commande

            // ── Vérifier session ─────────────────────────────────────────
            int userId = sessionManager.getUserIdForToken(token);
            if (userId == -1) {
                AppLogger.SECURITY.warn("Session expirée depuis IP {}",
                        clientSocket.getInetAddress().getHostAddress());
                return "ERROR:SESSION_EXPIRED";
            }

            if (!sessionManager.isValidSession(userId, token)) {
                AppLogger.SECURITY.warn("Session invalide pour userId={}", userId);
                return "ERROR:INVALID_SESSION";
            }

            // ── Vérifier rôle admin pour commandes ADMIN_* ───────────────
            if (command.startsWith("ADMIN_")) {
                String role = sessionManager.getRoleForToken(token);
                if (!"admin".equalsIgnoreCase(role)) {
                    AppLogger.SECURITY.warn("Accès admin refusé pour userId={}", userId);
                    return "ERROR:ACCESS_DENIED";
                }
            }

            // ── Routage avec 'command' (pas rawRequest) ──────────────────
            if (command.startsWith("GET_PROFILE:"))          return handleGetProfile(command);
            if (command.startsWith("UPDATE_PROFILE:"))       return handleUpdateProfile(command);

            if (command.startsWith("CART_ADD:"))             return handleCartAdd(command);
            if (command.startsWith("CART_REMOVE_BY_NAME:"))  return handleCartRemoveByName(command);
            if (command.startsWith("CART_REMOVE:"))          return handleCartRemove(command);
            if (command.startsWith("CART_GET:"))             return handleCartGet(command);
            if (command.startsWith("CART_CLEAR:"))           return handleCartClear(command);

            if (command.startsWith("CHECKOUT:"))             return handleCheckout(command);
            if (command.startsWith("PAYMENT:"))              return handlePayment(command);

            if (command.startsWith("ADMIN_ADD_PRODUCT:"))    return handleAdminAddProduct(command);
            if (command.startsWith("ADMIN_UPDATE_PRODUCT:")) return handleAdminUpdateProduct(command);
            if (command.startsWith("ADMIN_DELETE_PRODUCT:")) return handleAdminDeleteProduct(command);

            if (command.equalsIgnoreCase("ADMIN_GET_CATEGORIES"))        return handleAdminGetCategories();
            if (command.startsWith("ADMIN_ADD_CATEGORY:"))               return handleAdminAddCategory(command);
            if (command.startsWith("ADMIN_UPDATE_CATEGORY:"))            return handleAdminUpdateCategory(command);
            if (command.startsWith("ADMIN_DELETE_CATEGORY:"))            return handleAdminDeleteCategory(command);

            if (command.equalsIgnoreCase("ADMIN_GET_USERS"))             return handleAdminGetUsers();
            if (command.equalsIgnoreCase("ADMIN_GET_ORDERS"))            return handleAdminGetOrders();
            if (command.startsWith("ADMIN_UPDATE_ORDER_STATUS:"))        return handleAdminUpdateOrderStatus(command);

            if (command.equalsIgnoreCase("ADMIN_GET_DASHBOARD_SUMMARY")) return handleAdminGetDashboardSummary();
            if (command.equalsIgnoreCase("ADMIN_GET_NOTIFICATIONS"))     return handleAdminGetNotifications();
            if (command.startsWith("ADMIN_MARK_NOTIFICATION_READ:"))     return handleAdminMarkNotificationRead(command);
            if (command.equalsIgnoreCase("ADMIN_GET_STOCK_ALERTS"))      return handleAdminGetStockAlerts();
            if (command.equalsIgnoreCase("ADMIN_GET_STOCK_HISTORY"))     return handleAdminGetStockHistory();
            if (command.startsWith("ADMIN_ADJUST_STOCK:"))               return handleAdminAdjustStock(command);

            if (command.startsWith("ADMIN_SECURE_TEST:"))                return handleAdminSecureTest(command);

            AppLogger.SECURITY.warn("Commande inconnue : {}", command);
            return "ERROR:UNKNOWN_COMMAND";

        } catch (Exception e) {
            AppLogger.SERVER.error("Erreur traitement requête : {}", e.getMessage());
            e.printStackTrace();
            return "ERROR:EXCEPTION_OCCURRED";
        }
    }

    // =========================================================
    // AUTH / PROFILE
    // =========================================================

    private String handleLogin(String request) {
        try {
            String[] parts = request.split(":", 3);
            if (parts.length != 3) return "ERROR:LOGIN_FORMAT";

            String email      = parts[1];
            String password   = parts[2];
            String ip         = clientSocket.getInetAddress().getHostAddress();
            String limiterKey = email + "@" + ip;

            if (loginLimiter.isBlocked(limiterKey)) {
                AppLogger.SECURITY.warn("Connexion bloquée : email={}, ip={}", email, ip);
                return "ERROR:TOO_MANY_ATTEMPTS";
            }

            User user = authService.login(email, password);

            if (user != null) {
                loginLimiter.recordSuccess(limiterKey);
                String sessionToken = sessionManager.createSession(user.getId(), user.getRole());
                AppLogger.SECURITY.info("Connexion réussie : email={}, role={}", email, user.getRole());
                return "LOGIN_SUCCESS:" + user.getId() + ":" + user.getRole() + ":" + sessionToken;
            }

            if (authService.emailExists(email) && !authService.isAccountActive(email))
                return "ERROR:ACCOUNT_NOT_ACTIVE";

            loginLimiter.recordFailure(limiterKey);
            AppLogger.SECURITY.warn("Échec connexion : email={}", email);
            return "ERROR:LOGIN_FAILED";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:LOGIN_EXCEPTION";
        }
    }

    private String handleRegister(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 8) return "ERROR:REGISTER_FORMAT";

            if (authService.emailExists(parts[3])) return "ERROR:EMAIL_ALREADY_EXISTS";

            boolean success = authService.registerPending(
                    parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], parts[7]);
            if (!success) return "ERROR:REGISTER_FAILED";

            boolean otpSent = otpService.sendOtp(parts[3]);
            return otpSent ? "REGISTER_SUCCESS_OTP_SENT" : "REGISTER_SUCCESS_BUT_OTP_FAILED";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:REGISTER_EXCEPTION";
        }
    }

    private String handleSendOtp(String request) {
        try {
            String[] parts = request.split(":", 2);
            if (parts.length != 2) return "ERROR:SEND_OTP_FORMAT";
            return otpService.sendOtp(parts[1]) ? "OTP_SENT" : "ERROR:OTP_SEND_FAILED";
        } catch (Exception e) {
            return "ERROR:SEND_OTP_EXCEPTION";
        }
    }

    private String handleVerifyOtp(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 3) return "ERROR:VERIFY_OTP_FORMAT";
            return otpService.verifyOtp(parts[1], parts[2]) ? "OTP_VERIFIED" : "ERROR:OTP_INVALID";
        } catch (Exception e) {
            return "ERROR:VERIFY_OTP_EXCEPTION";
        }
    }

    private String handleGetProfile(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 2) return "ERROR:GET_PROFILE_FORMAT";

            int userId = Integer.parseInt(parts[1]);
            User user  = new UserDAO().findById(userId);
            if (user == null) return "ERROR:PROFILE_NOT_FOUND";

            String fullName = (user.getPrenom() == null ? "" : user.getPrenom()) +
                    ((user.getNom() == null || user.getNom().isBlank()) ? "" : " " + user.getNom());
            String phone = "", address = "", city = "";

            if (user instanceof model.Client c) {
                phone   = c.getPhone()   == null ? "" : c.getPhone();
                address = c.getAddress() == null ? "" : c.getAddress();
                city    = c.getVille()   == null ? "" : c.getVille();
            }

            return "PROFILE_DATA:" + safe(fullName.trim()) + ";" + safe(user.getEmail()) + ";"
                    + safe(phone) + ";" + safe(address) + ";" + safe(city) + ";" + safe(user.getRole());

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:GET_PROFILE_EXCEPTION";
        }
    }

    private String handleGetProfileByEmail(String request) {
        try {
            String[] parts = request.split(":", 2);
            if (parts.length != 2) return "ERROR:GET_PROFILE_BY_EMAIL_FORMAT";

            User user = authService.getUserByUsername(parts[1]);
            if (user == null) return "ERROR:USER_NOT_FOUND";

            String fullName = (user.getPrenom() == null ? "" : user.getPrenom()) +
                    ((user.getNom() == null || user.getNom().isBlank()) ? "" : " " + user.getNom());

            return "PROFILE_DATA:" +
                    safe(fullName.trim()) + ";" +
                    safe(user.getEmail()) + ";" +
                    ";" +   // phone vide
                    ";" +   // address vide
                    ";" +   // city vide
                    safe(user.getRole()) + ";" +
                    user.getId();

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:GET_PROFILE_BY_EMAIL_EXCEPTION";
        }
    }

    private String handleUpdateProfile(String request) {
        try {
            String[] parts = request.split(":", 7);
            if (parts.length != 7) return "ERROR:UPDATE_PROFILE_FORMAT";

            boolean ok = new UserDAO().updateProfile(
                    Integer.parseInt(parts[1]), parts[2], parts[3], parts[4], parts[5], parts[6]);
            return ok ? "UPDATE_PROFILE_SUCCESS" : "ERROR:UPDATE_PROFILE_FAILED";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:UPDATE_PROFILE_EXCEPTION";
        }
    }

    // =========================================================
    // RSA ADMIN AUTH
    // =========================================================

    private String handleAdminAuthRequest(String request) {
        try {
            String[] parts = request.split(":", 2);
            if (parts.length != 2) return "ERROR:ADMIN_AUTH_REQUEST_FORMAT";

            String email = parts[1];
            User user = authService.getUserByUsername(email);
            if (user == null)                              return "ERROR:USER_NOT_FOUND";
            if (!"admin".equalsIgnoreCase(user.getRole())) return "ERROR:NOT_ADMIN";

            String challenge = authService.generateAdminChallenge(email);
            if (challenge == null) return "ERROR:CHALLENGE_GENERATION_FAILED";

            return "CHALLENGE:" + challenge;

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_AUTH_REQUEST_EXCEPTION";
        }
    }

    private String handleAdminChallengeResponse(String request) {
        try {
            String[] parts = request.split(":", 4);
            if (parts.length != 4) return "ERROR:ADMIN_CHALLENGE_RESPONSE_FORMAT";

            boolean verified = authService.verifyAdminSignature(parts[1], parts[2], parts[3]);

            if (verified) {
                // ⭐ Créer session admin après vérification RSA réussie
                User user = authService.getUserByUsername(parts[1]);
                if (user != null) {
                    String sessionToken = sessionManager.createSession(user.getId(), "admin");
                    AppLogger.SECURITY.info("Session admin créée pour : {}", parts[1]);
                    return "ADMIN_AUTH_SUCCESS:" + user.getId() + ":admin:" + sessionToken;
                }
            }

            return "ADMIN_AUTH_FAILED";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_CHALLENGE_RESPONSE_EXCEPTION";
        }
    }

    private String handleGetKeystorePassword(String request) {
        try {
            String[] parts = request.split(":", 2);
            if (parts.length != 2) return "ERROR:GET_KEYSTORE_PASSWORD_FORMAT";

            UserCertificateInfo cert = new UserCertificateDAO().findActiveByEmail(parts[1]);
            if (cert == null) return "ERROR:CERT_NOT_FOUND";

            String password = cert.getKeystorePassword();
            if (password == null) return "ERROR:PASSWORD_NOT_FOUND";

            return "KEYSTORE_PASSWORD:" + password;

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:GET_KEYSTORE_PASSWORD_EXCEPTION";
        }
    }

    // =========================================================
    // CART
    // =========================================================

    private String handleCartAdd(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 4) return "ERROR:CART_ADD_FORMAT";

            int clientId  = Integer.parseInt(parts[1]);
            int productId = Integer.parseInt(parts[2]);
            int quantity  = Integer.parseInt(parts[3]);

            if (quantity <= 0) return "ERROR:INVALID_QUANTITY";

            Product product = productService.getProductById(productId);
            if (product == null) return "ERROR:PRODUCT_NOT_FOUND";
            if (product.getStock() < quantity) return "ERROR:INSUFFICIENT_STOCK";

            CartItem item = new CartItem();
            item.setProduct(product);
            item.setQuantity(quantity);

            return cartService.addItemToCart(clientId, item)
                    ? "CART_ADD_SUCCESS" : "ERROR:CART_ADD_FAILED";

        } catch (NumberFormatException e) {
            return "ERROR:INVALID_NUMBER_FORMAT";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:CART_ADD_EXCEPTION";
        }
    }

    private String handleCartRemove(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 3) return "ERROR:CART_REMOVE_FORMAT";
            return cartService.removeItemFromCart(
                    Integer.parseInt(parts[1]), Integer.parseInt(parts[2]))
                    ? "CART_REMOVE_SUCCESS" : "ERROR:CART_REMOVE_FAILED";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:CART_REMOVE_EXCEPTION";
        }
    }

    private String handleCartRemoveByName(String request) {
        try {
            String[] parts = request.split(":", 3);
            if (parts.length != 3) return "ERROR:CART_REMOVE_BY_NAME_FORMAT";

            Product product = productService.getProductByName(parts[2]);
            if (product == null) return "ERROR:PRODUCT_NOT_FOUND";

            return cartService.removeItemFromCart(Integer.parseInt(parts[1]), product.getIdProduct())
                    ? "CART_REMOVE_SUCCESS" : "ERROR:CART_REMOVE_FAILED";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:CART_REMOVE_BY_NAME_EXCEPTION";
        }
    }

    private String handleCartGet(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 2) return "ERROR:CART_GET_FORMAT";

            int clientId = Integer.parseInt(parts[1]);
            Cart cart    = cartService.getCartByClient(clientId);

            if (cart == null || cart.getItems() == null || cart.getItems().isEmpty())
                return "CART_EMPTY";

            StringBuilder response = new StringBuilder();
            response.append("CART_DETAILS")
                    .append("|CartID=").append(cart.getId())
                    .append("|Items=").append(cart.getItems().size())
                    .append("|Total=").append(cart.calculateTotal());

            for (CartItem item : cart.getItems()) {
                if (item.getProduct() != null) {
                    response.append("|ProductId=").append(item.getProduct().getIdProduct())
                            .append(",Product=").append(safe(item.getProduct().getName()))
                            .append(",Qty=").append(item.getQuantity())
                            .append(",Subtotal=").append(item.calculateSubtotal());
                } else {
                    response.append("|ProductId=0,Product=UNKNOWN,Qty=")
                            .append(item.getQuantity()).append(",Subtotal=0.0");
                }
            }
            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:CART_GET_EXCEPTION";
        }
    }

    private String handleCartClear(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 2) return "ERROR:CART_CLEAR_FORMAT";
            return cartService.clearCart(Integer.parseInt(parts[1]))
                    ? "CART_CLEAR_SUCCESS" : "ERROR:CART_CLEAR_FAILED";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:CART_CLEAR_EXCEPTION";
        }
    }

    // =========================================================
    // PRODUCTS
    // =========================================================

    private String handleGetProducts() {
        try {
            List<Product> products = productService.getAllProducts();
            if (products == null || products.isEmpty()) return "NO_PRODUCTS";

            StringBuilder sb = new StringBuilder();
            for (Product p : products) {
                String cat = p.getCategory() != null && p.getCategory().getName() != null
                        ? p.getCategory().getName() : "Sans catégorie";
                sb.append(p.getIdProduct()).append(";").append(safe(p.getName())).append(";")
                  .append(p.getPrice()).append(";").append(safe(p.getImage())).append(";")
                  .append(safe(cat)).append(";").append(p.getStock()).append("|");
            }
            return removeLastPipe(sb);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:GET_PRODUCTS_FAILED";
        }
    }

    private String handleGetProduct(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 2) return "ERROR:GET_PRODUCT_FORMAT";

            Product p = productService.getProductById(Integer.parseInt(parts[1]));
            if (p == null) return "ERROR:PRODUCT_NOT_FOUND";

            String cat = p.getCategory() != null && p.getCategory().getName() != null
                    ? p.getCategory().getName() : "Sans catégorie";

            return p.getIdProduct() + ";" + safe(p.getName()) + ";" + p.getPrice() + ";"
                    + safe(p.getDescription()) + ";" + p.getStock() + ";"
                    + safe(p.getImage()) + ";" + safe(cat);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:GET_PRODUCT_EXCEPTION";
        }
    }

    // =========================================================
    // CHECKOUT / PAYMENT
    // =========================================================

    private String handleCheckout(String request) {
        try {
            if (orderService == null) return "ERROR:ORDER_SERVICE_NOT_INITIALIZED";

            String[] parts = request.split(":");
            if (parts.length != 2) return "ERROR:CHECKOUT_FORMAT";

            int clientId = Integer.parseInt(parts[1]);
            Cart cart    = cartService.getCartByClient(clientId);
            if (cart == null || cart.getItems() == null || cart.getItems().isEmpty())
                return "ERROR:CART_EMPTY";

            Order order = orderService.createOrder(clientId, cart.getItems());
            cartService.clearCart(clientId);
            return "ORDER_CREATED;" + order.getOrderUUID() + ";" + order.getTotalPrice();

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:CHECKOUT_EXCEPTION";
        }
    }

    private String handlePayment(String request) {
        try {
            if (orderService == null) return "ERROR:ORDER_SERVICE_NOT_INITIALIZED";

            String[] parts = request.split(":");
            if (parts.length != 3) return "ERROR:PAYMENT_FORMAT";

            Order order = orderService.getOrderByUUID(parts[1]);
            if (order == null) return "ERROR:ORDER_NOT_FOUND";

            Payment payment = new Payment();
            payment.setOrderId(order.getId());
            payment.setMethod(parts[2]);
            payment.setAmount(order.getTotalPrice());
            payment.setStatus("pending");

            boolean success = paymentService.processPayment(payment);
            if (success) {
                orderService.updateStatus(order.getId(), "paid");
                return "PAYMENT_SUCCESS;" + order.getOrderUUID();
            }
            return "PAYMENT_FAILED;" + order.getOrderUUID();

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:PAYMENT_EXCEPTION";
        }
    }

    // =========================================================
    // ADMIN PRODUCTS
    // =========================================================

    private String handleAdminAddProduct(String request) {
        try {
            String[] parts = request.split(":", 7);
            if (parts.length != 7) return "ERROR:ADMIN_ADD_PRODUCT_FORMAT";

            Product product = new Product(0, parts[1], parts[2], parts[5],
                    Double.parseDouble(parts[3]), Integer.parseInt(parts[4]));
            product.setCategory(new Category(Integer.parseInt(parts[6]), "", ""));

            boolean success = productService.addProduct(product);
            if (success)
                notificationService.syncProductStockNotification(
                        productService.getProductById(product.getIdProduct()), 5);

            return success ? "ADMIN_ADD_PRODUCT_SUCCESS" : "ERROR:ADMIN_ADD_PRODUCT_FAILED";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_ADD_PRODUCT_EXCEPTION";
        }
    }

    private String handleAdminUpdateProduct(String request) {
        try {
            String[] parts = request.split(":", 8);
            if (parts.length != 8) return "ERROR:ADMIN_UPDATE_PRODUCT_FORMAT";

            Product product = new Product(Integer.parseInt(parts[1]), parts[2], parts[3], parts[6],
                    Double.parseDouble(parts[4]), Integer.parseInt(parts[5]));
            product.setCategory(new Category(Integer.parseInt(parts[7]), "", ""));

            boolean success = productService.updateProduct(product);
            if (success)
                notificationService.syncProductStockNotification(
                        productService.getProductById(Integer.parseInt(parts[1])), 5);

            return success ? "ADMIN_UPDATE_PRODUCT_SUCCESS" : "ERROR:ADMIN_UPDATE_PRODUCT_FAILED";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_UPDATE_PRODUCT_EXCEPTION";
        }
    }

    private String handleAdminDeleteProduct(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 2) return "ERROR:ADMIN_DELETE_PRODUCT_FORMAT";
            return productService.deleteProduct(Integer.parseInt(parts[1]))
                    ? "ADMIN_DELETE_PRODUCT_SUCCESS" : "ERROR:ADMIN_DELETE_PRODUCT_FAILED";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_DELETE_PRODUCT_EXCEPTION";
        }
    }

    // =========================================================
    // ADMIN CATEGORIES
    // =========================================================

    private String handleGetCategories() {
        try {
            List<Category> categories = new CategoryDAO().findAll();
            if (categories == null || categories.isEmpty()) return "NO_CATEGORIES";

            StringBuilder sb = new StringBuilder();
            for (Category c : categories)
                sb.append(c.getId()).append(";").append(safe(c.getName())).append(";")
                  .append(safe(c.getDescription())).append("|");
            return removeLastPipe(sb);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:GET_CATEGORIES_EXCEPTION";
        }
    }

    private String handleAdminGetCategories() {
        return handleGetCategories();
    }

    private String handleAdminAddCategory(String request) {
        try {
            String[] parts = request.split(":", 3);
            if (parts.length != 3) return "ERROR:ADMIN_ADD_CATEGORY_FORMAT";
            new CategoryDAO().save(new Category(0, parts[1], parts[2]));
            return "ADMIN_ADD_CATEGORY_SUCCESS";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_ADD_CATEGORY_EXCEPTION";
        }
    }

    private String handleAdminUpdateCategory(String request) {
        try {
            String[] parts = request.split(":", 4);
            if (parts.length != 4) return "ERROR:ADMIN_UPDATE_CATEGORY_FORMAT";
            new CategoryDAO().update(new Category(Integer.parseInt(parts[1]), parts[2], parts[3]));
            return "ADMIN_UPDATE_CATEGORY_SUCCESS";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_UPDATE_CATEGORY_EXCEPTION";
        }
    }

    private String handleAdminDeleteCategory(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 2) return "ERROR:ADMIN_DELETE_CATEGORY_FORMAT";
            new CategoryDAO().delete(Integer.parseInt(parts[1]));
            return "ADMIN_DELETE_CATEGORY_SUCCESS";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_DELETE_CATEGORY_EXCEPTION";
        }
    }

    // =========================================================
    // ADMIN USERS / ORDERS
    // =========================================================

    private String handleAdminGetUsers() {
        try {
            List<User> users = new UserDAO().findAll();
            if (users == null || users.isEmpty()) return "NO_USERS";

            StringBuilder sb = new StringBuilder();
            for (User u : users)
                sb.append(u.getId()).append(";").append(safe(u.getNom())).append(";")
                  .append(safe(u.getPrenom())).append(";").append(safe(u.getEmail())).append(";")
                  .append(safe(u.getRole())).append(";").append(safe(u.getStatus())).append("|");
            return removeLastPipe(sb);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_GET_USERS_EXCEPTION";
        }
    }

    private String handleAdminGetOrders() {
        try {
            List<Order> orders = new OrderDAO().findAll();
            if (orders == null || orders.isEmpty()) return "NO_ORDERS";

            StringBuilder sb = new StringBuilder();
            for (Order o : orders)
                sb.append(o.getId()).append(";").append(safe(o.getOrderUUID())).append(";")
                  .append(safe(o.getClientFullName())).append(";").append(safe(o.getClientEmail())).append(";")
                  .append(o.getTotalPrice()).append(";").append(safe(o.getStatus())).append(";")
                  .append(o.getCreatedAt()).append("|");
            return removeLastPipe(sb);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_GET_ORDERS_EXCEPTION";
        }
    }

    private String handleAdminUpdateOrderStatus(String request) {
        try {
            String[] parts = request.split(":", 3);
            if (parts.length != 3) return "ERROR:ADMIN_UPDATE_ORDER_STATUS_FORMAT";
            new OrderDAO().updateStatus(Integer.parseInt(parts[1]), parts[2]);
            return "ADMIN_UPDATE_ORDER_STATUS_SUCCESS";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_UPDATE_ORDER_STATUS_EXCEPTION";
        }
    }

    // =========================================================
    // ADMIN DASHBOARD V2
    // =========================================================

    private String handleAdminGetDashboardSummary() {
        try {
            DashboardSummary s = dashboardService.getDashboardSummary();
            return "DASHBOARD_SUMMARY:" + s.getTotalProducts() + ";" + s.getLowStockProducts() + ";"
                    + s.getOutOfStockProducts() + ";" + s.getTotalUsers() + ";" + s.getTotalOrders() + ";"
                    + s.getPendingOrders() + ";" + s.getPaidOrders() + ";" + s.getTodayRevenue() + ";"
                    + s.getMonthRevenue() + ";" + s.getUnreadNotifications();
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_GET_DASHBOARD_SUMMARY_EXCEPTION";
        }
    }

    private String handleAdminGetNotifications() {
        try {
            notificationService.syncLowStockNotifications();
            List<Notification> list = notificationService.getUnreadNotifications();
            if (list == null || list.isEmpty()) return "NO_NOTIFICATIONS";

            StringBuilder sb = new StringBuilder();
            for (Notification n : list)
                sb.append(n.getId()).append(";").append(safe(n.getTitle())).append(";")
                  .append(safe(n.getMessage())).append(";").append(safe(n.getType())).append(";")
                  .append(safe(n.getLevel())).append(";").append(n.isRead()).append(";")
                  .append(safe(n.getEntityType())).append(";")
                  .append(n.getEntityId() == null ? "" : n.getEntityId()).append(";")
                  .append(n.getCreatedAt()).append("|");
            return removeLastPipe(sb);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_GET_NOTIFICATIONS_EXCEPTION";
        }
    }

    private String handleAdminMarkNotificationRead(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 2) return "ERROR:ADMIN_MARK_NOTIFICATION_READ_FORMAT";
            return notificationService.markAsRead(Integer.parseInt(parts[1]))
                    ? "ADMIN_MARK_NOTIFICATION_READ_SUCCESS"
                    : "ERROR:ADMIN_MARK_NOTIFICATION_READ_FAILED";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_MARK_NOTIFICATION_READ_EXCEPTION";
        }
    }

    private String handleAdminGetStockAlerts() {
        try {
            List<StockAlert> alerts = stockService.getLowStockAlerts();
            if (alerts == null || alerts.isEmpty()) return "NO_STOCK_ALERTS";

            StringBuilder sb = new StringBuilder();
            for (StockAlert a : alerts)
                sb.append(a.getProductId()).append(";").append(safe(a.getProductName())).append(";")
                  .append(a.getCurrentStock()).append(";").append(a.getThreshold()).append(";")
                  .append(safe(a.getLevel())).append(";").append(safe(a.getStatus())).append(";")
                  .append(a.getCreatedAt()).append("|");
            return removeLastPipe(sb);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_GET_STOCK_ALERTS_EXCEPTION";
        }
    }

    private String handleAdminGetStockHistory() {
        try {
            List<StockMovement> history = stockService.getStockHistory();
            if (history == null || history.isEmpty()) return "NO_STOCK_HISTORY";

            StringBuilder sb = new StringBuilder();
            for (StockMovement m : history)
                sb.append(m.getId()).append(";").append(m.getProductId()).append(";")
                  .append(safe(m.getProductName())).append(";").append(safe(m.getMovementType())).append(";")
                  .append(m.getQuantity()).append(";").append(m.getPreviousStock()).append(";")
                  .append(m.getNewStock()).append(";").append(safe(m.getReason())).append(";")
                  .append(m.getAdminUserId() == null ? "" : m.getAdminUserId()).append(";")
                  .append(m.getCreatedAt()).append("|");
            return removeLastPipe(sb);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_GET_STOCK_HISTORY_EXCEPTION";
        }
    }

    private String handleAdminAdjustStock(String request) {
        try {
            String[] parts = request.split(":", 6);
            if (parts.length != 6) return "ERROR:ADMIN_ADJUST_STOCK_FORMAT";
            if (Integer.parseInt(parts[2]) <= 0) return "ERROR:INVALID_STOCK_QUANTITY";

            return stockService.adjustStock(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                    parts[3], parts[4], Integer.parseInt(parts[5]))
                    ? "ADMIN_ADJUST_STOCK_SUCCESS" : "ERROR:ADMIN_ADJUST_STOCK_FAILED";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_ADJUST_STOCK_EXCEPTION";
        }
    }

    // =========================================================
    // ANTI-REPLAY
    // =========================================================

    private String handleAdminGetNonce() {
        String nonce = nonceManager.generateNonce();
        AppLogger.SECURITY.info("Nonce généré");
        return "NONCE:" + nonce;
    }

    private String handleAdminSecureTest(String request) {
        try {
            String[] parts = request.split(":", 3);
            if (parts.length != 3) return "ERROR:ADMIN_SECURE_TEST_FORMAT";

            if (!nonceManager.consumeNonce(parts[1])) {
                AppLogger.SECURITY.warn("Replay Attack détectée");
                return "ERROR:REPLAY_ATTACK_DETECTED";
            }

            AppLogger.SECURITY.info("Commande anti-replay acceptée : {}", parts[2]);
            return "ADMIN_SECURE_TEST_SUCCESS";
        } catch (Exception e) {
            return "ERROR:ADMIN_SECURE_TEST_EXCEPTION";
        }
    }

    // =========================================================
    // UTILS
    // =========================================================

    private String safe(String value) {
        return CommandValidator.clean(value);
    }

    private String removeLastPipe(StringBuilder sb) {
        if (sb == null || sb.length() == 0) return "";
        if (sb.charAt(sb.length() - 1) == '|') sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            System.out.println("Erreur fermeture ressources : " + e.getMessage());
        }
    }
}