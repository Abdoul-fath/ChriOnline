package utils;

import java.util.ArrayList;

import java.util.List;

/**
 * Valide la robustesse des mots de passe selon des critères de sécurité.
 * Utilisé côté client (inscription) et éventuellement côté serveur.
 */
public class PasswordValidator {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final PasswordStrength strength;

        public ValidationResult(boolean valid, List<String> errors, PasswordStrength strength) {
            this.valid = valid;
            this.errors = errors;
            this.strength = strength;
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public PasswordStrength getStrength() { return strength; }
    }

    public enum PasswordStrength {
        WEAK("Faible", "❌"),
        MEDIUM("Moyen", "⚠️"),
        STRONG("Fort", "✅"),
        VERY_STRONG("Très fort", "🔒");

        private final String label;
        private final String icon;

        PasswordStrength(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }

        public String getLabel() { return label; }
        public String getIcon() { return icon; }
    }

    /**
     * Valide un mot de passe selon les critères de sécurité.
     */
    public static ValidationResult validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("Le mot de passe ne peut pas être vide");
            return new ValidationResult(false, errors, PasswordStrength.WEAK);
        }

        if (password.length() < MIN_LENGTH) {
            errors.add("Minimum " + MIN_LENGTH + " caractères");
        }
        if (password.length() > MAX_LENGTH) {
            errors.add("Maximum " + MAX_LENGTH + " caractères");
        }
        if (!password.matches(".*[A-Z].*")) {
            errors.add("Au moins 1 majuscule (A-Z)");
        }
        if (!password.matches(".*[a-z].*")) {
            errors.add("Au moins 1 minuscule (a-z)");
        }
        if (!password.matches(".*[0-9].*")) {
            errors.add("Au moins 1 chiffre (0-9)");
        }
        if (!password.matches(".*[@#$%^&+=!?*_-].*")) {
            errors.add("Au moins 1 caractère spécial (@#$%^&+=!?*_-)");
        }

        boolean valid = errors.isEmpty();
        PasswordStrength strength = calculateStrength(password);
        return new ValidationResult(valid, errors, strength);
    }

    private static PasswordStrength calculateStrength(String password) {
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[@#$%^&+=!?*_-].*")) score++;
        if (password.length() >= 16) score++;

        if (score <= 3) return PasswordStrength.WEAK;
        if (score <= 5) return PasswordStrength.MEDIUM;
        if (score <= 6) return PasswordStrength.STRONG;
        return PasswordStrength.VERY_STRONG;
    }

    public static String getRequirementsMessage() {
        return "Le mot de passe doit contenir :\n" +
               "  • Au moins " + MIN_LENGTH + " caractères\n" +
               "  • Au moins 1 majuscule (A-Z)\n" +
               "  • Au moins 1 minuscule (a-z)\n" +
               "  • Au moins 1 chiffre (0-9)\n" +
               "  • Au moins 1 caractère spécial (@#$%^&+=!?*_-)";
    }

    public static List<String> getRequirementsList() {
        List<String> requirements = new ArrayList<>();
        requirements.add("Au moins " + MIN_LENGTH + " caractères");
        requirements.add("Au moins 1 majuscule (A-Z)");
        requirements.add("Au moins 1 minuscule (a-z)");
        requirements.add("Au moins 1 chiffre (0-9)");
        requirements.add("Au moins 1 caractère spécial (@#$%^&+=!?*_-)");
        return requirements;
    }

    public static boolean hasMinLength(String password) {
        return password != null && password.length() >= MIN_LENGTH;
    }

    public static boolean hasUpperCase(String password) {
        return password != null && password.matches(".*[A-Z].*");
    }

    public static boolean hasLowerCase(String password) {
        return password != null && password.matches(".*[a-z].*");
    }

    public static boolean hasDigit(String password) {
        return password != null && password.matches(".*[0-9].*");
    }

    public static boolean hasSpecialChar(String password) {
        return password != null && password.matches(".*[@#$%^&+=!?*_-].*");
    }
}