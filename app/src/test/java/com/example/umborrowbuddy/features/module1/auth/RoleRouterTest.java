package com.example.umborrowbuddy.features.module1.auth;

import com.example.umborrowbuddy.MainActivity;
import com.example.umborrowbuddy.shared.model.UserRole;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link RoleRouter}.
 * Verifies role-based destination resolution for REPORTER, MAINTENANCE_STAFF, and ADMIN.
 */
public class RoleRouterTest {

    @Test
    public void reporterRole_routesToMainActivity() {
        assertEquals(MainActivity.class, RoleRouter.getDestinationClass(UserRole.REPORTER));
    }

    @Test
    public void maintenanceStaffRole_routesToStaffDashboardActivity() {
        assertEquals(StaffDashboardActivity.class, RoleRouter.getDestinationClass(UserRole.MAINTENANCE_STAFF));
    }

    @Test
    public void adminRole_routesToAdminDashboardActivity() {
        assertEquals(AdminDashboardActivity.class, RoleRouter.getDestinationClass(UserRole.ADMIN));
    }

    @Test
    public void nullRole_defaultsToMainActivity() {
        assertEquals(MainActivity.class, RoleRouter.getDestinationClass(null));
    }

    @Test
    public void stringRoleMapping_resolvesCorrectDestinations() {
        assertEquals(MainActivity.class, RoleRouter.getDestinationClass(UserRole.fromString("REPORTER")));
        assertEquals(StaffDashboardActivity.class, RoleRouter.getDestinationClass(UserRole.fromString("MAINTENANCE_STAFF")));
        assertEquals(AdminDashboardActivity.class, RoleRouter.getDestinationClass(UserRole.fromString("admin")));
        assertEquals(MainActivity.class, RoleRouter.getDestinationClass(UserRole.fromString("unknown")));
    }
}
