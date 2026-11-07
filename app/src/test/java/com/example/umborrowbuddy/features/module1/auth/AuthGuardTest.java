package com.example.umborrowbuddy.features.module1.auth;

import com.example.umborrowbuddy.shared.model.UserRole;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link AuthGuard}.
 */
public class AuthGuardTest {

    @Test
    public void nullEmail_returnsNotLoggedIn() {
        assertEquals(AuthGuard.AuthState.NOT_LOGGED_IN, AuthGuard.evaluate(null, false));
        assertEquals(AuthGuard.AuthState.NOT_LOGGED_IN, AuthGuard.evaluate(null, true));
    }

    @Test
    public void nonUmEmail_returnsNonUmEmail() {
        assertEquals(AuthGuard.AuthState.NON_UM_EMAIL, AuthGuard.evaluate("hacker@gmail.com", true));
        assertEquals(AuthGuard.AuthState.NON_UM_EMAIL, AuthGuard.evaluate("user@monash.edu.my", true));
        assertEquals(AuthGuard.AuthState.NON_UM_EMAIL, AuthGuard.evaluate("user@siswa.um.edu.my.fake.com", true));
    }

    @Test
    public void umEmailUnverified_returnsUnverifiedEmail() {
        assertEquals(AuthGuard.AuthState.UNVERIFIED_EMAIL, AuthGuard.evaluate("u2102000@siswa.um.edu.my", false));
        assertEquals(AuthGuard.AuthState.UNVERIFIED_EMAIL, AuthGuard.evaluate("lecturer@um.edu.my", false));
    }

    @Test
    public void verifiedUmEmail_returnsAuthenticatedAndVerified() {
        assertEquals(AuthGuard.AuthState.AUTHENTICATED_AND_VERIFIED,
                AuthGuard.evaluate("u2102000@siswa.um.edu.my", true));
        assertEquals(AuthGuard.AuthState.AUTHENTICATED_AND_VERIFIED,
                AuthGuard.evaluate("lecturer@um.edu.my", true));
    }

    @Test
    public void roleAuthorization_verifiesAllowedRoles() {
        assertTrue(AuthGuard.isRoleAuthorized(UserRole.REPORTER, UserRole.REPORTER, UserRole.ADMIN));
        assertTrue(AuthGuard.isRoleAuthorized(UserRole.MAINTENANCE_STAFF, UserRole.MAINTENANCE_STAFF, UserRole.ADMIN));
        assertTrue(AuthGuard.isRoleAuthorized(UserRole.ADMIN, UserRole.ADMIN));

        assertFalse(AuthGuard.isRoleAuthorized(UserRole.REPORTER, UserRole.MAINTENANCE_STAFF, UserRole.ADMIN));
        assertFalse(AuthGuard.isRoleAuthorized(UserRole.MAINTENANCE_STAFF, UserRole.ADMIN));
        assertFalse(AuthGuard.isRoleAuthorized(null, UserRole.ADMIN));
        assertFalse(AuthGuard.isRoleAuthorized(UserRole.ADMIN, (UserRole[]) null));
    }
}
