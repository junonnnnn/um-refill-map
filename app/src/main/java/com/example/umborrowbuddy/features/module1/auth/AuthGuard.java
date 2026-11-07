package com.example.umborrowbuddy.features.module1.auth;

import com.example.umborrowbuddy.shared.model.UserRole;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Enforces session, email verification, and role authorization checks for protected screens.
 * Ensures unauthenticated, unverified, non-UM, or unauthorized users cannot access features.
 */
public final class AuthGuard {

    public enum AuthState {
        NOT_LOGGED_IN,
        NON_UM_EMAIL,
        UNVERIFIED_EMAIL,
        AUTHENTICATED_AND_VERIFIED
    }

    private AuthGuard() {
        // Utility class
    }

    /**
     * Checks the eligibility state of the currently active Firebase session.
     */
    public static AuthState checkAuthState() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return AuthState.NOT_LOGGED_IN;
        }
        return evaluate(user.getEmail(), user.isEmailVerified());
    }

    /**
     * Pure evaluator for testing session validity without requiring a live FirebaseAuth instance.
     */
    public static AuthState evaluate(String email, boolean isEmailVerified) {
        if (email == null) {
            return AuthState.NOT_LOGGED_IN;
        }
        if (!UmEmailValidator.isValidUmEmail(email)) {
            return AuthState.NON_UM_EMAIL;
        }
        if (!isEmailVerified) {
            return AuthState.UNVERIFIED_EMAIL;
        }
        return AuthState.AUTHENTICATED_AND_VERIFIED;
    }

    /**
     * Checks if a user's role is permitted among the specified allowed roles.
     *
     * @param userRole The role assigned to the user.
     * @param allowedRoles Permitted roles for the target feature/screen.
     * @return true if userRole matches at least one of allowedRoles.
     */
    public static boolean isRoleAuthorized(UserRole userRole, UserRole... allowedRoles) {
        if (userRole == null || allowedRoles == null || allowedRoles.length == 0) {
            return false;
        }
        for (UserRole allowed : allowedRoles) {
            if (userRole == allowed) {
                return true;
            }
        }
        return false;
    }
}
