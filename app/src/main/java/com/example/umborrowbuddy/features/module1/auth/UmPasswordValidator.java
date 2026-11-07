package com.example.umborrowbuddy.features.module1.auth;

/**
 * Validates password strength and policy rules for Module 1.
 * Policy requirements:
 * 1. At least 8 characters in length.
 * 2. At least one uppercase letter (A-Z).
 * 3. At least one lowercase letter (a-z).
 * 4. At least one number (0-9).
 */
public final class UmPasswordValidator {

    public enum ValidationResult {
        VALID,
        EMPTY,
        TOO_SHORT,
        MISSING_UPPERCASE,
        MISSING_LOWERCASE,
        MISSING_NUMBER
    }

    private UmPasswordValidator() {
        // Utility class
    }

    /**
     * Validates a password against all security policy rules.
     * Passwords must NOT be trimmed prior to validation.
     *
     * @param password The raw password input.
     * @return ValidationResult indicating success or the specific rule that failed.
     */
    public static ValidationResult validate(String password) {
        if (password == null || password.isEmpty()) {
            return ValidationResult.EMPTY;
        }
        if (password.length() < 8) {
            return ValidationResult.TOO_SHORT;
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasNumber = false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowercase = true;
            } else if (Character.isDigit(c)) {
                hasNumber = true;
            }
            if (hasUppercase && hasLowercase && hasNumber) {
                break;
            }
        }

        if (!hasUppercase) {
            return ValidationResult.MISSING_UPPERCASE;
        }
        if (!hasLowercase) {
            return ValidationResult.MISSING_LOWERCASE;
        }
        if (!hasNumber) {
            return ValidationResult.MISSING_NUMBER;
        }

        return ValidationResult.VALID;
    }

    /**
     * Checks if the password satisfies all policy rules.
     *
     * @param password The raw password input.
     * @return true if valid, false otherwise.
     */
    public static boolean isValid(String password) {
        return validate(password) == ValidationResult.VALID;
    }
}
