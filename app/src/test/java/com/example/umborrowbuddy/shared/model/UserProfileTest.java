package com.example.umborrowbuddy.shared.model;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link UserProfile}.
 */
public class UserProfileTest {

    @Test
    public void validProfile_passesValidationAndIsComplete() {
        UserProfile profile = new UserProfile(
                "testUid123",
                "u2102000@siswa.um.edu.my",
                "Vincent Loh",
                "Faculty of Computer Science & IT",
                "Kolej Kediaman Raja Dr. Nazrin Shah (KK12)"
        );

        assertNull(profile.validate());
        assertTrue(profile.isComplete());
        assertEquals("testUid123", profile.getUid());
        assertEquals("Vincent Loh", profile.getDisplayName());
        assertEquals(UserRole.REPORTER.name(), profile.getRole());
        assertEquals(UserRole.REPORTER, profile.getRoleEnum());
    }

    @Test
    public void customRole_isPreserved() {
        UserProfile staffProfile = new UserProfile(
                "staffUid",
                "staff@um.edu.my",
                "Staff Member",
                "JPPH",
                "Off-Campus",
                UserRole.MAINTENANCE_STAFF.name()
        );

        assertEquals(UserRole.MAINTENANCE_STAFF.name(), staffProfile.getRole());
        assertEquals(UserRole.MAINTENANCE_STAFF, staffProfile.getRoleEnum());

        Map<String, Object> map = staffProfile.toMap();
        assertEquals("MAINTENANCE_STAFF", map.get("role"));
    }

    @Test
    public void legacyProfileWithoutRole_defaultsToReporter() {
        UserProfile profile = new UserProfile(); // Default Firestore POJO constructor
        profile.setUid("legacyUid");
        profile.setEmail("legacy@siswa.um.edu.my");

        assertEquals(UserRole.REPORTER.name(), profile.getRole());
        assertEquals(UserRole.REPORTER, profile.getRoleEnum());
    }

    @Test
    public void invalidRoleSet_defaultsToReporter() {
        UserProfile profile = new UserProfile();
        profile.setRole("INVALID_ROLE");
        assertEquals(UserRole.REPORTER.name(), profile.getRole());
    }

    @Test
    public void missingDisplayName_failsValidation() {
        UserProfile profile = new UserProfile(
                "testUid123",
                "u2102000@siswa.um.edu.my",
                "",
                "FSKTM",
                "KK12"
        );

        assertNotNull(profile.validate());
        assertEquals("Display name cannot be empty.", profile.validate());
        assertFalse(profile.isComplete());
    }

    @Test
    public void displayNameExceedingLimit_failsValidation() {
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 65; i++) {
            longName.append("a");
        }
        UserProfile profile = new UserProfile(
                "testUid123",
                "u2102000@siswa.um.edu.my",
                longName.toString(),
                "FSKTM",
                "KK12"
        );

        assertEquals("Display name cannot exceed 60 characters.", profile.validate());
    }

    @Test
    public void missingResidentialCollege_failsValidation() {
        UserProfile profile = new UserProfile(
                "testUid123",
                "u2102000@siswa.um.edu.my",
                "Vincent Loh",
                "FSKTM",
                "   "
        );

        assertEquals("Residential college cannot be empty.", profile.validate());
        assertFalse(profile.isComplete());
    }
}
