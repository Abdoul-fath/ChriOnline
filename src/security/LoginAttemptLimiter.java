package security;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class LoginAttemptLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BLOCK_MINUTES = 5;

    private final Map<String, Integer> attempts = new HashMap<>();
    private final Map<String, LocalDateTime> blockedUntil = new HashMap<>();

    public synchronized boolean isBlocked(String key) {
        LocalDateTime until = blockedUntil.get(key);

        if (until == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(until)) {
            blockedUntil.remove(key);
            attempts.remove(key);
            return false;
        }

        return true;
    }

    public synchronized void recordFailure(String key) {
        int count = attempts.getOrDefault(key, 0) + 1;
        attempts.put(key, count);

        if (count >= MAX_ATTEMPTS) {
            blockedUntil.put(key, LocalDateTime.now().plusMinutes(BLOCK_MINUTES));
        }
    }

    public synchronized void recordSuccess(String key) {
        attempts.remove(key);
        blockedUntil.remove(key);
    }
}