package security;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class NonceManager {

    private static final int NONCE_VALID_SECONDS = 60;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, LocalDateTime> nonces = new HashMap<>();

    public synchronized String generateNonce() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);

        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        nonces.put(nonce, LocalDateTime.now().plusSeconds(NONCE_VALID_SECONDS));

        return nonce;
    }

    public synchronized boolean consumeNonce(String nonce) {
        LocalDateTime expiresAt = nonces.get(nonce);

        if (expiresAt == null) {
            return false;
        }

        nonces.remove(nonce);

        return LocalDateTime.now().isBefore(expiresAt);
    }
}