package security;

import java.security.SecureRandom;
import java.util.Base64;

public class ChallengeManager {

    private static final SecureRandom random = new SecureRandom();

    public static String genererChallenge() {
        byte[] bytes = new byte[32]; // 256 bits
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}