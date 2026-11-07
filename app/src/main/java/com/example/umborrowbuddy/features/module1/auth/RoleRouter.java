package com.example.umborrowbuddy.features.module1.auth;

import android.app.Activity;
import android.content.Intent;

import com.example.umborrowbuddy.MainActivity;
import com.example.umborrowbuddy.shared.model.UserRole;

/**
 * Handles role-based navigation and routing for authenticated & verified users.
 * - REPORTER -> MainActivity (Shared User Destination)
 * - MAINTENANCE_STAFF -> StaffDashboardActivity (Staff Dashboard)
 * - ADMIN -> AdminDashboardActivity (Admin Control Panel)
 */
public final class RoleRouter {

    private RoleRouter() {
        // Utility class
    }

    /**
     * Resolves the appropriate destination Activity class for a given user role.
     *
     * @param role User's assigned role.
     * @return Target Activity class.
     */
    public static Class<? extends Activity> getDestinationClass(UserRole role) {
        if (role == null) {
            return MainActivity.class;
        }
        switch (role) {
            case MAINTENANCE_STAFF:
                return StaffDashboardActivity.class;
            case ADMIN:
                return AdminDashboardActivity.class;
            case REPORTER:
            default:
                return MainActivity.class;
        }
    }

    /**
     * Routes an existing profile to its role dashboard, or a missing profile to profile creation.
     *
     * @param activity Source Activity.
     * @param profile Retrieved profile, or null if the user has not created one.
     */
    public static void routeProfile(Activity activity, com.example.umborrowbuddy.shared.model.UserProfile profile) {
        if (profile == null) {
            activity.startActivity(new Intent(activity, com.example.umborrowbuddy.features.module1.profile.ProfileActivity.class));
            activity.finish();
            return;
        }
        routeUserByRole(activity, UserRole.requireValid(profile.getRole()));
    }

    public static void routeUserByRole(Activity activity, UserRole role) {
        if (activity == null) {
            return;
        }
        Class<? extends Activity> target = getDestinationClass(role);
        Intent intent = new Intent(activity, target);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
