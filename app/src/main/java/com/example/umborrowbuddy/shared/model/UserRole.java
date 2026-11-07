package com.example.umborrowbuddy.shared.model;

import java.util.Locale;

/**
 * Roles for UM Water Refill Station Reporting & Maintenance App.
 * - REPORTER: General UM students/staff reporting water station issues.
 * - MAINTENANCE_STAFF: Staff handling refill station repair and maintenance tasks.
 * - ADMIN: System administrator managing stations, staff, and system access.
 */
public enum UserRole {
    REPORTER,
    MAINTENANCE_STAFF,
    ADMIN;

    /**
     * Parses a required role without silently assigning a default.
     *
     * @param value String representation of the assigned role.
     * @return The recognized role.
     * @throws IllegalArgumentException if the role is missing or unrecognized.
     */
    public static UserRole requireValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing user role.");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user role.");
        }
    }

    /** Parses a role for legacy callers, defaulting to REPORTER when unrecognized. */
    public static UserRole fromString(String roleStr) {
        if (roleStr != null && !roleStr.trim().isEmpty()) {
            String normalized = roleStr.trim().toUpperCase(Locale.ROOT);
            for (UserRole role : values()) {
                if (role.name().equals(normalized)) {
                    return role;
                }
            }
        }
        return REPORTER;
    }
}
