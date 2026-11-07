package com.example.umborrowbuddy.features.module1.auth;

import com.example.umborrowbuddy.shared.model.UserProfile;
import com.example.umborrowbuddy.shared.model.UserRole;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles Firebase Authentication and Cloud Firestore Profile & Role operations.
 */
public class AuthRepository {

    public interface Callback<T> {
        void onSuccess(T result);
        void onFailure(String errorMessage);
    }

    /**
     * Encapsulates registration outcome, separating account creation from verification email delivery.
     */
    public static class RegistrationResult {
        private final FirebaseUser user;
        private final boolean verificationEmailSent;
        private final String verificationErrorMessage;

        public RegistrationResult(FirebaseUser user, boolean verificationEmailSent, String verificationErrorMessage) {
            this.user = user;
            this.verificationEmailSent = verificationEmailSent;
            this.verificationErrorMessage = verificationErrorMessage;
        }

        public FirebaseUser getUser() {
            return user;
        }

        public boolean isVerificationEmailSent() {
            return verificationEmailSent;
        }

        public String getVerificationErrorMessage() {
            return verificationErrorMessage;
        }
    }

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public AuthRepository() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    public AuthRepository(FirebaseAuth auth, FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public String getCurrentUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Registers a new account and sends a verification email.
     * Initial Firestore profile creation is deferred until after email verification to satisfy email_verified rules.
     */
    public void register(String email, String password, Callback<RegistrationResult> callback) {
        String normalized = UmEmailValidator.normalizeEmail(email);
        auth.createUserWithEmailAndPassword(normalized, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        user.sendEmailVerification()
                                .addOnSuccessListener(aVoid -> callback.onSuccess(new RegistrationResult(user, true, null)))
                                .addOnFailureListener(e -> {
                                    String errMsg = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Verification email failed to send.";
                                    callback.onSuccess(new RegistrationResult(user, false, errMsg));
                                });
                    } else {
                        callback.onFailure("Registration succeeded, but user session could not be established.");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Registration failed."));
    }

    public void sendEmailVerification(Callback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure("No active user session found.");
            return;
        }
        user.sendEmailVerification()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Failed to send verification email."));
    }

    public void checkEmailVerification(Callback<Boolean> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure("No active user session found.");
            return;
        }
        user.reload()
                .addOnSuccessListener(aVoid -> {
                    if (user.isEmailVerified()) {
                        // Force refresh JWT ID token so claims (including email_verified) update in memory
                        user.getIdToken(true)
                                .addOnSuccessListener(result -> callback.onSuccess(true))
                                .addOnFailureListener(e -> callback.onSuccess(true));
                    } else {
                        callback.onSuccess(false);
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Failed to refresh verification status."));
    }

    public void login(String email, String password, Callback<FirebaseUser> callback) {
        String normalized = UmEmailValidator.normalizeEmail(email);
        auth.signInWithEmailAndPassword(normalized, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        // Force refresh ID token so email_verified claims sync immediately with Firestore rules
                        user.getIdToken(true)
                                .addOnSuccessListener(result -> callback.onSuccess(user))
                                .addOnFailureListener(e -> callback.onSuccess(user));
                    } else {
                        callback.onFailure("Sign-in succeeded, but user session could not be established.");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Invalid credentials or sign-in failed."));
    }

    public void sendPasswordReset(String email, Callback<Void> callback) {
        String normalized = UmEmailValidator.normalizeEmail(email);
        auth.sendPasswordResetEmail(normalized)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Failed to request password reset."));
    }

    public void logout() {
        auth.signOut();
    }

    public void getUserProfile(String uid, Callback<UserProfile> callback) {
        if (uid == null || uid.trim().isEmpty()) {
            callback.onFailure("User ID is required.");
            return;
        }
        firestore.collection("users").document(uid).get(Source.SERVER)
                .addOnSuccessListener(documentSnapshot -> {
                    Object rawValue = documentSnapshot.get("role");
                    String rawRole = rawValue instanceof String ? (String) rawValue : null;
                    if (documentSnapshot.exists()) {
                        try {
                            UserRole.requireValid(rawRole);
                        } catch (IllegalArgumentException e) {
                            callback.onFailure("User profile role is missing or invalid. Contact an administrator.");
                            return;
                        }
                    }
                    if (documentSnapshot.exists()) {
                        UserProfile profile = documentSnapshot.toObject(UserProfile.class);
                        if (profile != null) {
                            profile.setUid(documentSnapshot.getId());
                            if (documentSnapshot.contains("role")) {
                                profile.setRole(documentSnapshot.getString("role"));
                            }
                        }
                        deliverProfile(rawRole, profile, null, callback);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    deliverProfile(null, null, e, callback);
                });
    }

    static void deliverProfile(String rawRole, UserProfile profile, Exception error, Callback<UserProfile> callback) {
        if (error != null) {
            callback.onFailure("Failed to load user profile. Check your connection and try again.");
            return;
        }
        try {
            UserRole.requireValid(rawRole);
        } catch (IllegalArgumentException e) {
            callback.onFailure("User profile role is missing or invalid. Contact an administrator.");
            return;
        }
        if (profile == null) {
            callback.onFailure("Unable to read user profile.");
            return;
        }
        profile.setRole(rawRole);
        callback.onSuccess(profile);
    }

    /**
     * Saves user profile updates while strictly preserving role security (preventing self-assignment).
     * Initializes default REPORTER role for new user documents upon verified profile creation.
     */
    public void saveUserProfile(UserProfile profile, Callback<Void> callback) {
        if (profile == null || profile.getUid() == null) {
            callback.onFailure("Invalid profile data.");
            return;
        }
        String uid = profile.getUid();

        firestore.collection("users").document(uid).get(Source.SERVER)
                .addOnSuccessListener(snapshot -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("uid", uid);
                    data.put("email", profile.getEmail());
                    data.put("displayName", profile.getDisplayName());
                    data.put("faculty", profile.getFaculty());
                    data.put("residentialCollege", profile.getResidentialCollege());
                    data.put("updatedAt", FieldValue.serverTimestamp());

                    // Only initialize a new profile; never write an existing user\'s role.
                    if (!snapshot.exists()) {
                        data.put("role", UserRole.REPORTER.name());
                    }

                    if (!snapshot.exists() || snapshot.get("createdAt") == null) {
                        data.put("createdAt", FieldValue.serverTimestamp());
                    } else {
                        // Preserve original createdAt timestamp
                        data.put("createdAt", snapshot.get("createdAt"));
                    }

                    firestore.collection("users").document(uid).set(data, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onFailure(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Failed to save profile."));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Failed to verify existing profile."));
    }
}
