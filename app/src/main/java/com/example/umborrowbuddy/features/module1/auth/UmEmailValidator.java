package com.example.umborrowbuddy.features.module1.auth;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validates and normalizes Universiti Malaya (UM) student and staff email addresses.
 * Allowed domains: siswa.um.edu.my (students) and um.edu.my (staff).
 */
public final class UmEmailValidator {

    private static final String ALLOWED_DOMAIN_STUDENT = "siswa.um.edu.my";
    private static final String ALLOWED_DOMAIN_STAFF = "um.edu.my";

    // Standard RFC-compliant email pattern that runs in both JVM unit tests and Android runtime
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    private UmEmailValidator() {
        // Utility class
    }

    /**
     * Checks if the given email is a valid UM email address.
     * Rejects null, empty strings, invalid email syntax, subdomains, and lookalike domains.
     *
     * @param email The raw email input.
     * @return true if the email is valid and belongs to siswa.um.edu.my or um.edu.my.
     */
    public static boolean isValidUmEmail(String email) {
        if (email == null) {
            return false;
        }
        String trimmed = email.trim();
        if (trimmed.isEmpty() || !EMAIL_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        int atIndex = trimmed.lastIndexOf('@');
        if (atIndex <= 0 || atIndex == trimmed.length() - 1) {
            return false;
        }
        String domain = trimmed.substring(atIndex + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_DOMAIN_STUDENT.equals(domain) || ALLOWED_DOMAIN_STAFF.equals(domain);
    }

    /**
     * Normalizes an email address by trimming whitespace and lowercasing the domain.
     *
     * @param email The raw email input.
     * @return The normalized email string, or empty string if input is null.
     */
    public static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        String trimmed = email.trim();
        int atIndex = trimmed.lastIndexOf('@');
        if (atIndex > 0 && atIndex < trimmed.length() - 1) {
            String localPart = trimmed.substring(0, atIndex);
            String domainPart = trimmed.substring(atIndex + 1).toLowerCase(Locale.ROOT);
            return localPart + "@" + domainPart;
        }
        return trimmed;
    }
}
