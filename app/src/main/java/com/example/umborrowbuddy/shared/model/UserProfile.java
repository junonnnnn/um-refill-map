package com.example.umborrowbuddy.shared.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared User Profile model stored in Cloud Firestore under `users/{firebaseAuthUid}`.
 * Serves as the primary user entity across all modules in UM Water Refill Station Reporting System.
 */
public class UserProfile {

    private String uid;
    private String email;
    private String displayName;
    private String faculty;
    private String residentialCollege;
    private String role = UserRole.REPORTER.name();

    @ServerTimestamp
    private Date createdAt;

    @ServerTimestamp
    private Date updatedAt;

    /** Required public no-argument constructor for Firestore POJO deserialization. */
    public UserProfile() {
    }

    public UserProfile(String uid, String email, String displayName, String faculty,
                       String residentialCollege) {
        this(uid, email, displayName, faculty, residentialCollege, UserRole.REPORTER.name());
    }

    public UserProfile(String uid, String email, String displayName, String faculty,
                       String residentialCollege, String role) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.faculty = faculty;
        this.residentialCollege = residentialCollege;
        setRole(role);
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public String getResidentialCollege() {
        return residentialCollege;
    }

    public void setResidentialCollege(String residentialCollege) {
        this.residentialCollege = residentialCollege;
    }

    public String getRole() {
        return role != null ? role : UserRole.REPORTER.name();
    }

    public void setRole(String role) {
        this.role = UserRole.fromString(role).name();
    }

    @Exclude
    public UserRole getRoleEnum() {
        return UserRole.fromString(role);
    }

    @Exclude
    public void setRoleEnum(UserRole userRole) {
        this.role = userRole != null ? userRole.name() : UserRole.REPORTER.name();
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Checks if all mandatory profile fields are filled.
     */
    @Exclude
    public boolean isComplete() {
        return displayName != null && !displayName.trim().isEmpty()
                && faculty != null && !faculty.trim().isEmpty()
                && residentialCollege != null && !residentialCollege.trim().isEmpty();
    }

    /**
     * Validates field constraints according to PRD contract.
     * @return null if valid, or an error message describing the violation.
     */
    @Exclude
    public String validate() {
        if (displayName == null || displayName.trim().isEmpty()) {
            return "Display name cannot be empty.";
        }
        if (displayName.trim().length() > 60) {
            return "Display name cannot exceed 60 characters.";
        }
        if (faculty == null || faculty.trim().isEmpty()) {
            return "Faculty cannot be empty.";
        }
        if (faculty.trim().length() > 100) {
            return "Faculty cannot exceed 100 characters.";
        }
        if (residentialCollege == null || residentialCollege.trim().isEmpty()) {
            return "Residential college cannot be empty.";
        }
        if (residentialCollege.trim().length() > 100) {
            return "Residential college cannot exceed 100 characters.";
        }
        return null;
    }

    /**
     * Converts to a map for Firestore updates, preserving existing createdAt and role.
     */
    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("email", email);
        map.put("displayName", displayName != null ? displayName.trim() : "");
        map.put("faculty", faculty != null ? faculty.trim() : "");
        map.put("residentialCollege", residentialCollege != null ? residentialCollege.trim() : "");
        map.put("role", getRole());
        if (createdAt != null) {
            map.put("createdAt", createdAt);
        }
        if (updatedAt != null) {
            map.put("updatedAt", updatedAt);
        }
        return map;
    }
}
