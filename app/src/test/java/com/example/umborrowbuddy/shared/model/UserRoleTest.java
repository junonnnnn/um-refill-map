package com.example.umborrowbuddy.shared.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link UserRole}.
 */
public class UserRoleTest {

    @Test
    public void validRoleStrings_parseCorrectly() {
        assertEquals(UserRole.REPORTER, UserRole.fromString("REPORTER"));
        assertEquals(UserRole.MAINTENANCE_STAFF, UserRole.fromString("MAINTENANCE_STAFF"));
        assertEquals(UserRole.ADMIN, UserRole.fromString("ADMIN"));
    }

    @Test
    public void caseInsensitiveAndWhitespace_parsesCorrectly() {
        assertEquals(UserRole.REPORTER, UserRole.fromString("  reporter  "));
        assertEquals(UserRole.MAINTENANCE_STAFF, UserRole.fromString("maintenance_staff"));
        assertEquals(UserRole.ADMIN, UserRole.fromString("admin"));
    }

    @Test
    public void invalidOrNullRoleStrings_defaultToReporter() {
        assertEquals(UserRole.REPORTER, UserRole.fromString(null));
        assertEquals(UserRole.REPORTER, UserRole.fromString(""));
        assertEquals(UserRole.REPORTER, UserRole.fromString("   "));
        assertEquals(UserRole.REPORTER, UserRole.fromString("SUPER_USER"));
        assertEquals(UserRole.REPORTER, UserRole.fromString("HACKER"));
    }
}
