package security;

import java.util.Set;

public class CommandValidator {

    private static final int MAX_REQUEST_LENGTH = 3000;

    private static final Set<String> ALLOWED_COMMANDS = Set.of(
            "PING",
            "LOGIN",
            "REGISTER",
            "SEND_OTP",
            "VERIFY_OTP",
            "GET_PROFILE",
            "UPDATE_PROFILE",
            "GET_PROFILE_BY_EMAIL",
            "ADMIN_AUTH_REQUEST",
            "ADMIN_CHALLENGE_RESPONSE",
            "GET_CATEGORIES",
            "GET_PRODUCTS",
            "GET_PRODUCT",
            "CART_ADD",
            "CART_REMOVE",
            "CART_REMOVE_BY_NAME",
            "CART_GET",
            "CART_CLEAR",
            "CHECKOUT",
            "PAYMENT",
            "ADMIN_ADD_PRODUCT",
            "ADMIN_UPDATE_PRODUCT",
            "ADMIN_DELETE_PRODUCT",
            "ADMIN_GET_CATEGORIES",
            "ADMIN_ADD_CATEGORY",
            "ADMIN_UPDATE_CATEGORY",
            "ADMIN_DELETE_CATEGORY",
            "ADMIN_GET_USERS",
            "ADMIN_GET_ORDERS",
            "ADMIN_UPDATE_ORDER_STATUS",
            "ADMIN_GET_DASHBOARD_SUMMARY",
            "ADMIN_GET_NOTIFICATIONS",
            "ADMIN_MARK_NOTIFICATION_READ",
            "ADMIN_GET_STOCK_ALERTS",
            "ADMIN_GET_STOCK_HISTORY",
            "ADMIN_ADJUST_STOCK",
            "ADMIN_GET_NONCE",
            "ADMIN_SECURE_TEST"
    );

    public static boolean isValidRequest(String request) {
        if (request == null || request.isBlank()) {
            return false;
        }

        if (request.length() > MAX_REQUEST_LENGTH) {
            return false;
        }

        String command = request.contains(":")
                ? request.substring(0, request.indexOf(":"))
                : request;

        return ALLOWED_COMMANDS.contains(command);
    }

    public static String clean(String value) {
        if (value == null) return "";

        return value
                .replace(";", ",")
                .replace("|", "/")
                .replace(":", "-")
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();
    }
}