package com.example.umborrowbuddy.features.module1.auth;

import com.example.umborrowbuddy.shared.model.UserProfile;
import com.example.umborrowbuddy.shared.model.UserRole;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProfileRoleRetrievalTest {
    private static class Result implements AuthRepository.Callback<UserProfile> {
        UserProfile profile;
        String error;
        public void onSuccess(UserProfile value) { profile = value; }
        public void onFailure(String value) { error = value; }
    }

    @Test public void allServerRolesSurviveDefaultProfileAndReachCorrectDestination() {
        for (UserRole role : UserRole.values()) {
            Result result = new Result();
            AuthRepository.deliverProfile(role.name(), new UserProfile(), null, result);
            assertNull(result.error);
            assertEquals(role, result.profile.getRoleEnum());
            assertEquals(RoleRouter.getDestinationClass(role),
                    RoleRouter.getDestinationClass(result.profile.getRoleEnum()));
        }
    }

    @Test public void failedReadNeverDeliversReporter() {
        Result result = new Result();
        AuthRepository.deliverProfile("REPORTER", new UserProfile(), new Exception("read failed"), result);
        assertNotNull(result.error);
        assertNull(result.profile);
    }

    @Test public void missingAndInvalidRolesNeverDeliverReporter() {
        for (String role : new String[] {null, "", " ", "UNKNOWN"}) {
            Result result = new Result();
            AuthRepository.deliverProfile(role, new UserProfile(), null, result);
            assertNotNull(result.error);
            assertNull(result.profile);
        }
    }

    @Test public void strictParserSupportsCaseAndWhitespace() {
        assertEquals(UserRole.MAINTENANCE_STAFF, UserRole.requireValid(" maintenance_staff "));
    }
}
