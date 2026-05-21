package security;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

public class SecureSessionManager {

    private static final int SESSION_MINUTES = 1; // ← changer à 30 pour production

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, SessionData> sessions = new ConcurrentHashMap<>();

    // =========================================================
    // CRÉER UNE SESSION
    // =========================================================

    public synchronized String createSession(int userId, String role) {
        // Supprimer l'ancienne session de cet utilisateur
        sessions.entrySet().removeIf(e -> e.getValue().userId == userId);

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        sessions.put(token, new SessionData(
                userId,
                role,
                LocalDateTime.now().plusMinutes(SESSION_MINUTES)
        ));

        System.out.println("✅ Session créée : userId=" + userId + " role=" + role
                + " expire dans " + SESSION_MINUTES + " min");
        return token;
    }

    // =========================================================
    // VÉRIFIER UNE SESSION
    // =========================================================

    public synchronized boolean isValidSession(int userId, String token) {
        SessionData session = sessions.get(token);
        if (session == null) return false;
        if (session.userId != userId) return false;

        if (LocalDateTime.now().isAfter(session.expiresAt)) {
            sessions.remove(token);
            System.out.println("⏰ Session expirée pour userId=" + userId);
            return false;
        }

        // ⭐ PAS de sliding window — expiration fixe depuis le login
        return true;
    }

    public synchronized boolean isValidAdminSession(String token) {
        SessionData session = sessions.get(token);
        if (session == null) return false;

        if (LocalDateTime.now().isAfter(session.expiresAt)) {
            sessions.remove(token);
            System.out.println("⏰ Session admin expirée");
            return false;
        }

        // ⭐ PAS de sliding window
        return "admin".equalsIgnoreCase(session.role);
    }

    // =========================================================
    // RÉCUPÉRER INFOS SESSION
    // =========================================================

    public synchronized int getUserIdForToken(String token) {
        SessionData session = sessions.get(token);
        if (session == null) return -1;

        if (LocalDateTime.now().isAfter(session.expiresAt)) {
            sessions.remove(token);
            return -1;
        }

        return session.userId;
    }

    public synchronized String getRoleForToken(String token) {
        SessionData session = sessions.get(token);
        if (session == null) return null;

        if (LocalDateTime.now().isAfter(session.expiresAt)) {
            sessions.remove(token);
            return null;
        }

        return session.role;
    }

    // =========================================================
    // SUPPRIMER SESSION
    // =========================================================

    public synchronized void removeSession(String token) {
        SessionData session = sessions.remove(token);
        if (session != null)
            System.out.println("🗑️ Session supprimée pour userId=" + session.userId);
    }

    public synchronized void removeSessionByUserId(int userId) {
        sessions.entrySet().removeIf(e -> {
            if (e.getValue().userId == userId) {
                System.out.println("🗑️ Session supprimée pour userId=" + userId);
                return true;
            }
            return false;
        });
    }

    public synchronized void cleanExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        int before = sessions.size();
        sessions.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt));
        int removed = before - sessions.size();
        if (removed > 0)
            System.out.println("🧹 " + removed + " session(s) expirée(s) supprimée(s)");
    }

    // =========================================================
    // SESSION DATA
    // =========================================================

    private static class SessionData {
        int userId;
        String role;
        LocalDateTime expiresAt;

        SessionData(int userId, String role, LocalDateTime expiresAt) {
            this.userId    = userId;
            this.role      = role;
            this.expiresAt = expiresAt;
        }
    }
}