package server;

import dao.CategoryDAO;
import dao.OrderDAO;
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

    private static final LoginAttemptLimiter loginLimiter = new LoginAttemptLimiter();
    private static final SecureSessionManager sessionManager = new SecureSessionManager();
    private static final NonceManager nonceManager = new NonceManager();

    private final Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    private final CartService cartService;
    private final ProductService productService;
    private OrderService orderService;
    private final PaymentService paymentService;
    private final AuthService authService;
    private final OtpService otpService;

    private final NotificationService notificationService;
    private final StockService stockService;
    private final DashboardService dashboardService;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.cartService = new CartService();
        this.productService = new ProductService();
        this.paymentService = new PaymentService();
        this.authService = new AuthService();
        this.otpService = new OtpService();

        this.notificationService = new NotificationService();
        this.stockService = new StockService();
        this.dashboardService = new DashboardService();

        try {
            this.orderService = new OrderService();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Impossible d'initialiser OrderService");
            this.orderService = null;
        }

        try {
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
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

                if (response != null) {
                    out.println(response);
                }
            }

        } catch (IOException e) {
            AppLogger.SERVER.warn("Client déconnecté : {}", ip);
        } finally {
            closeResources();
        }
    }

    private String handleRequest(String request) {
        try {
            if (request == null || request.trim().isEmpty()) {
                return "ERROR:EMPTY_REQUEST";
            }

            if (!CommandValidator.isValidRequest(request)) {
                AppLogger.SECURITY.warn("Commande invalide bloquée depuis {} : {}",
                        clientSocket.getInetAddress().getHostAddress(), request);
                return "ERROR:INVALID_COMMAND";
            }

            if (request.equalsIgnoreCase("PING")) return "PONG";

            // AUTH
            if (request.startsWith("LOGIN:")) return handleLogin(request);
            if (request.startsWith("REGISTER:")) return handleRegister(request);
            if (request.startsWith("SEND_OTP:")) return handleSendOtp(request);
            if (request.startsWith("VERIFY_OTP:")) return handleVerifyOtp(request);

            // PROFILE
            if (request.startsWith("GET_PROFILE:")) return handleGetProfile(request);
            if (request.startsWith("UPDATE_PROFILE:")) return handleUpdateProfile(request);
            if (request.startsWith("GET_PROFILE_BY_EMAIL:")) return handleGetProfileByEmail(request);

            // RSA ADMIN AUTH
            if (request.startsWith("ADMIN_AUTH_REQUEST:")) return handleAdminAuthRequest(request);
            if (request.startsWith("ADMIN_CHALLENGE_RESPONSE:")) return handleAdminChallengeResponse(request);

            // PUBLIC
            if (request.equalsIgnoreCase("GET_CATEGORIES")) return handleGetCategories();
            if (request.equalsIgnoreCase("GET_PRODUCTS")) return handleGetProducts();
            if (request.startsWith("GET_PRODUCT:")) return handleGetProduct(request);

            // CART
            if (request.startsWith("CART_ADD:")) return handleCartAdd(request);
            if (request.startsWith("CART_REMOVE:")) return handleCartRemove(request);
            if (request.startsWith("CART_REMOVE_BY_NAME:")) return handleCartRemoveByName(request);
            if (request.startsWith("CART_GET:")) return handleCartGet(request);
            if (request.startsWith("CART_CLEAR:")) return handleCartClear(request);

            // CHECKOUT / PAYMENT
            if (request.startsWith("CHECKOUT:")) return handleCheckout(request);
            if (request.startsWith("PAYMENT:")) return handlePayment(request);

            // ADMIN PRODUCTS
            if (request.startsWith("ADMIN_ADD_PRODUCT:")) return handleAdminAddProduct(request);
            if (request.startsWith("ADMIN_UPDATE_PRODUCT:")) return handleAdminUpdateProduct(request);
            if (request.startsWith("ADMIN_DELETE_PRODUCT:")) return handleAdminDeleteProduct(request);

            // ADMIN CATEGORIES
            if (request.equalsIgnoreCase("ADMIN_GET_CATEGORIES")) return handleAdminGetCategories();
            if (request.startsWith("ADMIN_ADD_CATEGORY:")) return handleAdminAddCategory(request);
            if (request.startsWith("ADMIN_UPDATE_CATEGORY:")) return handleAdminUpdateCategory(request);
            if (request.startsWith("ADMIN_DELETE_CATEGORY:")) return handleAdminDeleteCategory(request);

            // ADMIN USERS / ORDERS
            if (request.equalsIgnoreCase("ADMIN_GET_USERS")) return handleAdminGetUsers();
            if (request.equalsIgnoreCase("ADMIN_GET_ORDERS")) return handleAdminGetOrders();
            if (request.startsWith("ADMIN_UPDATE_ORDER_STATUS:")) return handleAdminUpdateOrderStatus(request);

            // ADMIN DASHBOARD V2
            if (request.equalsIgnoreCase("ADMIN_GET_DASHBOARD_SUMMARY")) return handleAdminGetDashboardSummary();
            if (request.equalsIgnoreCase("ADMIN_GET_NOTIFICATIONS")) return handleAdminGetNotifications();
            if (request.startsWith("ADMIN_MARK_NOTIFICATION_READ:")) return handleAdminMarkNotificationRead(request);
            if (request.equalsIgnoreCase("ADMIN_GET_STOCK_ALERTS")) return handleAdminGetStockAlerts();
            if (request.equalsIgnoreCase("ADMIN_GET_STOCK_HISTORY")) return handleAdminGetStockHistory();
            if (request.startsWith("ADMIN_ADJUST_STOCK:")) return handleAdminAdjustStock(request);

            // ANTI-REPLAY (TP2)
            if (request.equalsIgnoreCase("ADMIN_GET_NONCE")) return handleAdminGetNonce();
            if (request.startsWith("ADMIN_SECURE_TEST:")) return handleAdminSecureTest(request);

            AppLogger.SECURITY.warn("Commande inconnue : {}", request);
            return "ERROR:UNKNOWN_COMMAND";

        } catch (Exception e) {
            AppLogger.SERVER.error("Erreur traitement requête : {}", e.getMessage());
            e.printStackTrace();
            return "ERROR:EXCEPTION_OCCURRED";
        }
    }

    // =========================================================
    // PUBLIC / AUTH / PROFILE
    // =========================================================

    private String handleGetCategories() {
        try {
            CategoryDAO categoryDAO = new CategoryDAO();
            List<Category> categories = categoryDAO.findAll();

            if (categories == null || categories.isEmpty()) {
                return "NO_CATEGORIES";
            }

            StringBuilder sb = new StringBuilder();
            for (Category c : categories) {
                sb.append(c.getId()).append(";")
                        .append(safe(c.getName())).append(";")
                        .append(safe(c.getDescription()))
                        .append("|");
            }

            return removeLastPipe(sb);

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:GET_CATEGORIES_EXCEPTION";
        }
    }

    private String handleLogin(String request) {
        try {
            String[] parts = request.split(":", 3);
            if (parts.length != 3) return "ERROR:LOGIN_FORMAT";

            String email = parts[1];
            String password = parts[2];
            String ip = clientSocket.getInetAddress().getHostAddress();

            String limiterKey = email + "@" + ip;

            if (loginLimiter.isBlocked(limiterKey)) {
                AppLogger.SECURITY.warn("Connexion bloquée temporairement : email={}, ip={}", email, ip);
                return "ERROR:TOO_MANY_ATTEMPTS";
            }

            User user = authService.login(email, password);

            if (user != null) {
                loginLimiter.recordSuccess(limiterKey);

                String sessionToken = sessionManager.createSession(user.getId(), user.getRole());

                AppLogger.SECURITY.info("Connexion réussie : email={}, role={}, ip={}",
                        email, user.getRole(), ip);

                return "LOGIN_SUCCESS:" + user.getId() + ":" + user.getRole() + ":" + sessionToken;
            }

            if (authService.emailExists(email) && !authService.isAccountActive(email)) {
                AppLogger.SECURITY.warn("Compte non actif : email={}, ip={}", email, ip);
                return "ERROR:ACCOUNT_NOT_ACTIVE";
            }

            loginLimiter.recordFailure(limiterKey);

            AppLogger.SECURITY.warn("Échec connexion : email={}, ip={}", email, ip);

            return "ERROR:LOGIN_FAILED";

        } catch (Exception e) {
            AppLogger.SECURITY.error("Erreur login : {}", e.getMessage());
            e.printStackTrace();
            return "ERROR:LOGIN_EXCEPTION";
        }
    }

    private String handleRegister(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 8) return "ERROR:REGISTER_FORMAT";

            String nom = parts[1];
            String prenom = parts[2];
            String email = parts[3];
            String password = parts[4];
            String address = parts[5];
            String phone = parts[6];
            String ville = parts[7];

            if (authService.emailExists(email)) {
                return "ERROR:EMAIL_ALREADY_EXISTS";
            }

            boolean success = authService.registerPending(nom, prenom, email, password, address, phone, ville);
            if (!success) {
                return "ERROR:REGISTER_FAILED";
            }

            boolean otpSent = otpService.sendOtp(email);
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

            String email = parts[1];
            boolean sent = otpService.sendOtp(email);

            return sent ? "OTP_SENT" : "ERROR:OTP_SEND_FAILED";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:SEND_OTP_EXCEPTION";
        }
    }

    private String handleVerifyOtp(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 3) return "ERROR:VERIFY_OTP_FORMAT";

            String email = parts[1];
            String code = parts[2];

            boolean verified = otpService.verifyOtp(email, code);
            return verified ? "OTP_VERIFIED" : "ERROR:OTP_INVALID";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:VERIFY_OTP_EXCEPTION";
        }
    }

    private String handleGetProfile(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 2) return "ERROR:GET_PROFILE_FORMAT";

            int userId = Integer.parseInt(parts[1]);

            UserDAO userDAO = new UserDAO();
            User user = userDAO.findById(userId);

            if (user == null) {
                return "ERROR:PROFILE_NOT_FOUND";
            }

            String fullName = (user.getPrenom() == null ? "" : user.getPrenom()) +
                    ((user.getNom() == null || user.getNom().isBlank()) ? "" : " " + user.getNom());

            String phone = "";
            String address = "";
            String city = "";

            if (user instanceof model.Client client) {
                phone = client.getPhone() == null ? "" : client.getPhone();
                address = client.getAddress() == null ? "" : client.getAddress();
                city = client.getVille() == null ? "" : client.getVille();
            }

            return "PROFILE_DATA:" +
                    safe(fullName.trim()) + ";" +
                    safe(user.getEmail()) + ";" +
                    safe(phone) + ";" +
                    safe(address) + ";" +
                    safe(city) + ";" +
                    safe(user.getRole());

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:GET_PROFILE_EXCEPTION";
        }
    }

    private String handleGetProfileByEmail(String request) {
        try {
            String[] parts = request.split(":", 2);
            if (parts.length != 2) return "ERROR:GET_PROFILE_BY_EMAIL_FORMAT";

            String email = parts[1];
            User user = authService.getUserByUsername(email);

            if (user == null) {
                return "ERROR:USER_NOT_FOUND";
            }

            String fullName = (user.getPrenom() == null ? "" : user.getPrenom()) +
                    ((user.getNom() == null || user.getNom().isBlank()) ? "" : " " + user.getNom());

            return "PROFILE_DATA:" +
                    safe(fullName.trim()) + ";" +
                    safe(user.getEmail()) + ";" +
                    "" + ";" +
                    "" + ";" +
                    "" + ";" +
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

            int userId = Integer.parseInt(parts[1]);
            String fullName = parts[2];
            String email = parts[3];
            String phone = parts[4];
            String address = parts[5];
            String city = parts[6];

            UserDAO userDAO = new UserDAO();
            boolean success = userDAO.updateProfile(userId, fullName, email, phone, address, city);

            return success ? "UPDATE_PROFILE_SUCCESS" : "ERROR:UPDATE_PROFILE_FAILED";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:UPDATE_PROFILE_EXCEPTION";
        }
    }

    // =========================================================
    // RSA ADMIN AUTHENTICATION
    // =========================================================

    private String handleAdminAuthRequest(String request) {
        try {
            String[] parts = request.split(":", 2);
            if (parts.length != 2) return "ERROR:ADMIN_AUTH_REQUEST_FORMAT";

            String email = parts[1];

            User user = authService.getUserByUsername(email);
            if (user == null) {
                return "ERROR:USER_NOT_FOUND";
            }

            if (!"admin".equalsIgnoreCase(user.getRole())) {
                return "ERROR:NOT_ADMIN";
            }

            String challenge = authService.generateAdminChallenge(email);
            if (challenge == null) {
                return "ERROR:CHALLENGE_GENERATION_FAILED";
            }

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

            String email = parts[1];
            String signature = parts[2];
            String challenge = parts[3];

            boolean verified = authService.verifyAdminSignature(email, signature, challenge);

            return verified ? "ADMIN_AUTH_SUCCESS" : "ADMIN_AUTH_FAILED";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_CHALLENGE_RESPONSE_EXCEPTION";
        }
    }

    // =========================================================
    // CART
    // =========================================================

    private String handleCartAdd(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 4) return "ERROR:CART_ADD_FORMAT";

            int clientId = Integer.parseInt(parts[1]);
            int productId = Integer.parseInt(parts[2]);
            int quantity = Integer.parseInt(parts[3]);

            if (quantity <= 0) return "ERROR:INVALID_QUANTITY";

            Product product = productService.getProductById(productId);
            if (product == null) return "ERROR:PRODUCT_NOT_FOUND";
            if (product.getStock() < quantity) return "ERROR:INSUFFICIENT_STOCK";

            CartItem item = new CartItem();
            item.setProduct(product);
            item.setQuantity(quantity);

            boolean added = cartService.addItemToCart(clientId, item);
            return added ? "CART_ADD_SUCCESS" : "ERROR:CART_ADD_FAILED";

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

            int clientId = Integer.parseInt(parts[1]);
            int productId = Integer.parseInt(parts[2]);

            boolean removed = cartService.removeItemFromCart(clientId, productId);
            return removed ? "CART_REMOVE_SUCCESS" : "ERROR:CART_REMOVE_FAILED";

        } catch (NumberFormatException e) {
            return "ERROR:INVALID_NUMBER_FORMAT";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:CART_REMOVE_EXCEPTION";
        }
    }

    private String handleCartRemoveByName(String request) {
        try {
            String[] parts = request.split(":", 3);
            if (parts.length != 3) return "ERROR:CART_REMOVE_BY_NAME_FORMAT";

            int clientId = Integer.parseInt(parts[1]);
            String productName = parts[2];

            Product product = productService.getProductByName(productName);
            if (product == null) {
                return "ERROR:PRODUCT_NOT_FOUND";
            }

            boolean removed = cartService.removeItemFromCart(clientId, product.getIdProduct());
            return removed ? "CART_REMOVE_SUCCESS" : "ERROR:CART_REMOVE_FAILED";

        } catch (NumberFormatException e) {
            return "ERROR:INVALID_NUMBER_FORMAT";
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
            Cart cart = cartService.getCartByClient(clientId);

            if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
                return "CART_EMPTY";
            }

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
                    response.append("|ProductId=0,Product=UNKNOWN,Qty=").append(item.getQuantity())
                            .append(",Subtotal=0.0");
                }
            }

            return response.toString();

        } catch (NumberFormatException e) {
            return "ERROR:INVALID_NUMBER_FORMAT";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:CART_GET_EXCEPTION";
        }
    }

    private String handleCartClear(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 2) return "ERROR:CART_CLEAR_FORMAT";

            int clientId = Integer.parseInt(parts[1]);
            boolean cleared = cartService.clearCart(clientId);

            return cleared ? "CART_CLEAR_SUCCESS" : "ERROR:CART_CLEAR_FAILED";

        } catch (NumberFormatException e) {
            return "ERROR:INVALID_NUMBER_FORMAT";
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

            if (products == null || products.isEmpty()) {
                return "NO_PRODUCTS";
            }

            StringBuilder sb = new StringBuilder();

            for (Product p : products) {
                String categoryName = "Sans catégorie";
                if (p.getCategory() != null && p.getCategory().getName() != null) {
                    categoryName = p.getCategory().getName();
                }

                sb.append(p.getIdProduct()).append(";")
                        .append(safe(p.getName())).append(";")
                        .append(p.getPrice()).append(";")
                        .append(safe(p.getImage())).append(";")
                        .append(safe(categoryName)).append(";")
                        .append(p.getStock())
                        .append("|");
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

            int productId = Integer.parseInt(parts[1]);
            Product p = productService.getProductById(productId);

            if (p == null) return "ERROR:PRODUCT_NOT_FOUND";

            String categoryName = "Sans catégorie";
            if (p.getCategory() != null && p.getCategory().getName() != null) {
                categoryName = p.getCategory().getName();
            }

            return p.getIdProduct() + ";" +
                    safe(p.getName()) + ";" +
                    p.getPrice() + ";" +
                    safe(p.getDescription()) + ";" +
                    p.getStock() + ";" +
                    safe(p.getImage()) + ";" +
                    safe(categoryName);

        } catch (NumberFormatException e) {
            return "ERROR:INVALID_NUMBER_FORMAT";
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
            Cart cart = cartService.getCartByClient(clientId);

            if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
                return "ERROR:CART_EMPTY";
            }

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

            String orderUUID = parts[1];
            String method = parts[2];

            Order order = orderService.getOrderByUUID(orderUUID);
            if (order == null) return "ERROR:ORDER_NOT_FOUND";

            Payment payment = new Payment();
            payment.setOrderId(order.getId());
            payment.setMethod(method);
            payment.setAmount(order.getTotalPrice());
            payment.setStatus("pending");

            boolean success = paymentService.processPayment(payment);

            if (success) {
                orderService.updateStatus(order.getId(), "paid");
                return "PAYMENT_SUCCESS;" + order.getOrderUUID();
            } else {
                return "PAYMENT_FAILED;" + order.getOrderUUID();
            }

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

            String name = parts[1];
            String description = parts[2];
            double price = Double.parseDouble(parts[3]);
            int stock = Integer.parseInt(parts[4]);
            String image = parts[5];
            int categoryId = Integer.parseInt(parts[6]);

            Product product = new Product(0, name, description, image, price, stock);
            product.setCategory(new Category(categoryId, "", ""));

            boolean success = productService.addProduct(product);

            if (success) {
                notificationService.syncProductStockNotification(productService.getProductById(product.getIdProduct()), 5);
            }

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

            int id = Integer.parseInt(parts[1]);
            String name = parts[2];
            String description = parts[3];
            double price = Double.parseDouble(parts[4]);
            int stock = Integer.parseInt(parts[5]);
            String image = parts[6];
            int categoryId = Integer.parseInt(parts[7]);

            Product product = new Product(id, name, description, image, price, stock);
            product.setCategory(new Category(categoryId, "", ""));

            boolean success = productService.updateProduct(product);

            if (success) {
                notificationService.syncProductStockNotification(productService.getProductById(id), 5);
            }

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

            int id = Integer.parseInt(parts[1]);

            boolean success = productService.deleteProduct(id);
            return success ? "ADMIN_DELETE_PRODUCT_SUCCESS" : "ERROR:ADMIN_DELETE_PRODUCT_FAILED";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_DELETE_PRODUCT_EXCEPTION";
        }
    }

    // =========================================================
    // ADMIN CATEGORIES
    // =========================================================

    private String handleAdminGetCategories() {
        try {
            CategoryDAO categoryDAO = new CategoryDAO();
            List<Category> categories = categoryDAO.findAll();

            if (categories == null || categories.isEmpty()) {
                return "NO_CATEGORIES";
            }

            StringBuilder sb = new StringBuilder();
            for (Category c : categories) {
                sb.append(c.getId()).append(";")
                        .append(safe(c.getName())).append(";")
                        .append(safe(c.getDescription()))
                        .append("|");
            }

            return removeLastPipe(sb);

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_GET_CATEGORIES_EXCEPTION";
        }
    }

    private String handleAdminAddCategory(String request) {
        try {
            String[] parts = request.split(":", 3);
            if (parts.length != 3) return "ERROR:ADMIN_ADD_CATEGORY_FORMAT";

            CategoryDAO categoryDAO = new CategoryDAO();
            Category category = new Category(0, parts[1], parts[2]);
            categoryDAO.save(category);

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

            CategoryDAO categoryDAO = new CategoryDAO();
            Category category = new Category(Integer.parseInt(parts[1]), parts[2], parts[3]);
            categoryDAO.update(category);

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

            CategoryDAO categoryDAO = new CategoryDAO();
            categoryDAO.delete(Integer.parseInt(parts[1]));

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
            UserDAO userDAO = new UserDAO();
            List<User> users = userDAO.findAll();

            if (users == null || users.isEmpty()) {
                return "NO_USERS";
            }

            StringBuilder sb = new StringBuilder();
            for (User user : users) {
                sb.append(user.getId()).append(";")
                        .append(safe(user.getNom())).append(";")
                        .append(safe(user.getPrenom())).append(";")
                        .append(safe(user.getEmail())).append(";")
                        .append(safe(user.getRole())).append(";")
                        .append(safe(user.getStatus()))
                        .append("|");
            }

            return removeLastPipe(sb);

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_GET_USERS_EXCEPTION";
        }
    }

    private String handleAdminGetOrders() {
        try {
            OrderDAO orderDAO = new OrderDAO();
            List<Order> orders = orderDAO.findAll();

            if (orders == null || orders.isEmpty()) {
                return "NO_ORDERS";
            }

            StringBuilder sb = new StringBuilder();

            for (Order order : orders) {
                sb.append(order.getId()).append(";")
                        .append(safe(order.getOrderUUID())).append(";")
                        .append(safe(order.getClientFullName())).append(";")
                        .append(safe(order.getClientEmail())).append(";")
                        .append(order.getTotalPrice()).append(";")
                        .append(safe(order.getStatus())).append(";")
                        .append(order.getCreatedAt())
                        .append("|");
            }

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

            int orderId = Integer.parseInt(parts[1]);
            String status = parts[2];

            OrderDAO orderDAO = new OrderDAO();
            orderDAO.updateStatus(orderId, status);

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
            DashboardSummary summary = dashboardService.getDashboardSummary();

            return "DASHBOARD_SUMMARY:" +
                    summary.getTotalProducts() + ";" +
                    summary.getLowStockProducts() + ";" +
                    summary.getOutOfStockProducts() + ";" +
                    summary.getTotalUsers() + ";" +
                    summary.getTotalOrders() + ";" +
                    summary.getPendingOrders() + ";" +
                    summary.getPaidOrders() + ";" +
                    summary.getTodayRevenue() + ";" +
                    summary.getMonthRevenue() + ";" +
                    summary.getUnreadNotifications();

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_GET_DASHBOARD_SUMMARY_EXCEPTION";
        }
    }

    private String handleAdminGetNotifications() {
        try {
            notificationService.syncLowStockNotifications();

            List<Notification> notifications = notificationService.getUnreadNotifications();

            if (notifications == null || notifications.isEmpty()) {
                return "NO_NOTIFICATIONS";
            }

            StringBuilder sb = new StringBuilder();

            for (Notification n : notifications) {
                sb.append(n.getId()).append(";")
                        .append(safe(n.getTitle())).append(";")
                        .append(safe(n.getMessage())).append(";")
                        .append(safe(n.getType())).append(";")
                        .append(safe(n.getLevel())).append(";")
                        .append(n.isRead()).append(";")
                        .append(safe(n.getEntityType())).append(";")
                        .append(n.getEntityId() == null ? "" : n.getEntityId()).append(";")
                        .append(n.getCreatedAt())
                        .append("|");
            }

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

            int notificationId = Integer.parseInt(parts[1]);
            boolean success = notificationService.markAsRead(notificationId);

            return success
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

            if (alerts == null || alerts.isEmpty()) {
                return "NO_STOCK_ALERTS";
            }

            StringBuilder sb = new StringBuilder();

            for (StockAlert a : alerts) {
                sb.append(a.getProductId()).append(";")
                        .append(safe(a.getProductName())).append(";")
                        .append(a.getCurrentStock()).append(";")
                        .append(a.getThreshold()).append(";")
                        .append(safe(a.getLevel())).append(";")
                        .append(safe(a.getStatus())).append(";")
                        .append(a.getCreatedAt())
                        .append("|");
            }

            return removeLastPipe(sb);

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_GET_STOCK_ALERTS_EXCEPTION";
        }
    }

    private String handleAdminGetStockHistory() {
        try {
            List<StockMovement> history = stockService.getStockHistory();

            if (history == null || history.isEmpty()) {
                return "NO_STOCK_HISTORY";
            }

            StringBuilder sb = new StringBuilder();

            for (StockMovement m : history) {
                sb.append(m.getId()).append(";")
                        .append(m.getProductId()).append(";")
                        .append(safe(m.getProductName())).append(";")
                        .append(safe(m.getMovementType())).append(";")
                        .append(m.getQuantity()).append(";")
                        .append(m.getPreviousStock()).append(";")
                        .append(m.getNewStock()).append(";")
                        .append(safe(m.getReason())).append(";")
                        .append(m.getAdminUserId() == null ? "" : m.getAdminUserId()).append(";")
                        .append(m.getCreatedAt())
                        .append("|");
            }

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

            int productId = Integer.parseInt(parts[1]);
            int quantity = Integer.parseInt(parts[2]);
            String movementType = parts[3];
            String reason = parts[4];
            Integer adminUserId = Integer.parseInt(parts[5]);

            if (quantity <= 0) {
                return "ERROR:INVALID_STOCK_QUANTITY";
            }

            boolean success = stockService.adjustStock(productId, quantity, movementType, reason, adminUserId);

            return success
                    ? "ADMIN_ADJUST_STOCK_SUCCESS"
                    : "ERROR:ADMIN_ADJUST_STOCK_FAILED";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:ADMIN_ADJUST_STOCK_EXCEPTION";
        }
    }

    // =========================================================
    // ANTI-REPLAY (TP2)
    // =========================================================

    private String handleAdminGetNonce() {
        String nonce = nonceManager.generateNonce();
        AppLogger.SECURITY.info("Nonce généré pour protection anti-replay");
        return "NONCE:" + nonce;
    }

    private String handleAdminSecureTest(String request) {
        try {
            String[] parts = request.split(":", 3);

            if (parts.length != 3) {
                return "ERROR:ADMIN_SECURE_TEST_FORMAT";
            }

            String nonce = parts[1];
            String message = parts[2];

            boolean valid = nonceManager.consumeNonce(nonce);

            if (!valid) {
                AppLogger.SECURITY.warn("Replay Attack détectée : nonce réutilisé ou expiré");
                return "ERROR:REPLAY_ATTACK_DETECTED";
            }

            AppLogger.SECURITY.info("Commande anti-replay acceptée : {}", message);
            return "ADMIN_SECURE_TEST_SUCCESS";

        } catch (Exception e) {
            AppLogger.SECURITY.error("Erreur anti-replay : {}", e.getMessage());
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
        if (sb == null || sb.length() == 0) {
            return "";
        }
        if (sb.charAt(sb.length() - 1) == '|') {
            sb.deleteCharAt(sb.length() - 1);
        }
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