package security;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SecureSessionManager {

    private static final int SESSION_MINUTES = 30;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, SessionData> sessions = new HashMap<>();

    public synchronized String createSession(int userId, String role) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        sessions.put(token, new SessionData(
                userId,
                role,
                LocalDateTime.now().plusMinutes(SESSION_MINUTES)
        ));

        return token;
    }

    public synchronized boolean isValidAdminSession(String token) {
        SessionData session = sessions.get(token);

        if (session == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(session.expiresAt)) {
            sessions.remove(token);
            return false;
        }

        return "admin".equalsIgnoreCase(session.role);
    }

    public synchronized void removeSession(String token) {
        sessions.remove(token);
    }

    private static class SessionData {
        int userId;
        String role;
        LocalDateTime expiresAt;

        SessionData(int userId, String role, LocalDateTime expiresAt) {
            this.userId = userId;
            this.role = role;
            this.expiresAt = expiresAt;
        }
    }
}