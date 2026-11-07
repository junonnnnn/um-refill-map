package com.example.umborrowbuddy.features.module1.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link UmEmailValidator}.
 */
public class UmEmailValidatorTest {

    @Test
    public void validStudentEmail_returnsTrue() {
        assertTrue(UmEmailValidator.isValidUmEmail("u2102000@siswa.um.edu.my"));
        assertTrue(UmEmailValidator.isValidUmEmail("vincent.loh@siswa.um.edu.my"));
        assertTrue(UmEmailValidator.isValidUmEmail("25005653@siswa.um.edu.my"));
    }

    @Test
    public void validStaffEmail_returnsTrue() {
        assertTrue(UmEmailValidator.isValidUmEmail("lecturer@um.edu.my"));
        assertTrue(UmEmailValidator.isValidUmEmail("prof.smith@um.edu.my"));
        assertTrue(UmEmailValidator.isValidUmEmail("dean_office@um.edu.my"));
    }

    @Test
    public void caseInsensitiveDomain_returnsTrue() {
        assertTrue(UmEmailValidator.isValidUmEmail("student@SISWA.UM.EDU.MY"));
        assertTrue(UmEmailValidator.isValidUmEmail("staff@Um.Edu.My"));
    }

    @Test
    public void emailWithSurroundingWhitespace_returnsTrue() {
        assertTrue(UmEmailValidator.isValidUmEmail("  vincent@siswa.um.edu.my  "));
    }

    @Test
    public void lookalikeDomains_returnsFalse() {
        assertFalse(UmEmailValidator.isValidUmEmail("user@siswa.um.edu.my.attacker.com"));
        assertFalse(UmEmailValidator.isValidUmEmail("user@um.edu.my.fake.org"));
        assertFalse(UmEmailValidator.isValidUmEmail("user@um.edu.my.com"));
    }

    @Test
    public void unapprovedSubdomains_returnsFalse() {
        assertFalse(UmEmailValidator.isValidUmEmail("user@cs.siswa.um.edu.my"));
        assertFalse(UmEmailValidator.isValidUmEmail("user@fsktm.um.edu.my"));
    }

    @Test
    public void commercialAndOtherUniversities_returnsFalse() {
        assertFalse(UmEmailValidator.isValidUmEmail("user@gmail.com"));
        assertFalse(UmEmailValidator.isValidUmEmail("user@yahoo.com"));
        assertFalse(UmEmailValidator.isValidUmEmail("user@monash.edu.my"));
        assertFalse(UmEmailValidator.isValidUmEmail("user@ukm.edu.my"));
    }

    @Test
    public void malformedAndEmptyEmails_returnsFalse() {
        assertFalse(UmEmailValidator.isValidUmEmail(null));
        assertFalse(UmEmailValidator.isValidUmEmail(""));
        assertFalse(UmEmailValidator.isValidUmEmail("   "));
        assertFalse(UmEmailValidator.isValidUmEmail("user@"));
        assertFalse(UmEmailValidator.isValidUmEmail("@siswa.um.edu.my"));
        assertFalse(UmEmailValidator.isValidUmEmail("user@@siswa.um.edu.my"));
        assertFalse(UmEmailValidator.isValidUmEmail("user siswa.um.edu.my"));
    }

    @Test
    public void normalizeEmail_cleansWhitespaceAndLowercasesDomain() {
        assertEquals("user@siswa.um.edu.my", UmEmailValidator.normalizeEmail("  user@SISWA.UM.EDU.MY  "));
        assertEquals("staff@um.edu.my", UmEmailValidator.normalizeEmail("staff@UM.EDU.MY"));
        assertEquals("", UmEmailValidator.normalizeEmail(null));
    }

    @Test
    public void module1_acceptsValidUmEduMyEmails_andRejectsInvalidDomains() {
        // Confirms valid @um.edu.my email formats are strictly accepted
        assertTrue(UmEmailValidator.isValidUmEmail("staff@um.edu.my"));
        assertTrue(UmEmailValidator.isValidUmEmail("lecturer.cs@um.edu.my"));
        assertTrue(UmEmailValidator.isValidUmEmail("office_admin@um.edu.my"));
        assertTrue(UmEmailValidator.isValidUmEmail("chancellor-office@um.edu.my"));
        assertTrue(UmEmailValidator.isValidUmEmail("DEAN@UM.EDU.MY"));

        // Confirms invalid, lookalike, or non-UM domains are strictly rejected
        assertFalse(UmEmailValidator.isValidUmEmail("lecturer@gmail.com"));
        assertFalse(UmEmailValidator.isValidUmEmail("staff@yahoo.com"));
        assertFalse(UmEmailValidator.isValidUmEmail("lecturer@um.edu"));
        assertFalse(UmEmailValidator.isValidUmEmail("staff@um.edu.my.attacker.com"));
        assertFalse(UmEmailValidator.isValidUmEmail("admin@fsktm.um.edu.my"));
        assertFalse(UmEmailValidator.isValidUmEmail("lecturer@utm.my"));
        assertFalse(UmEmailValidator.isValidUmEmail("lecturer@usm.my"));
        assertFalse(UmEmailValidator.isValidUmEmail("staff@notum.edu.my"));
    }
}
