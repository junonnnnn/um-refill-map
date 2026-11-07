package com.example.umborrowbuddy.features.module1.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link UmPasswordValidator}.
 * Verifies password policy rules:
 * - At least 8 characters
 * - At least 1 uppercase letter (A-Z)
 * - At least 1 lowercase letter (a-z)
 * - At least 1 number (0-9)
 */
public class UmPasswordValidatorTest {

    @Test
    public void validPasswords_passValidation() {
        assertEquals(UmPasswordValidator.ValidationResult.VALID, UmPasswordValidator.validate("Password123"));
        assertEquals(UmPasswordValidator.ValidationResult.VALID, UmPasswordValidator.validate("Abcdefg1"));
        assertEquals(UmPasswordValidator.ValidationResult.VALID, UmPasswordValidator.validate("P@ssword1"));
        assertEquals(UmPasswordValidator.ValidationResult.VALID, UmPasswordValidator.validate("StrongP4ss!"));

        assertTrue(UmPasswordValidator.isValid("Password123"));
        assertTrue(UmPasswordValidator.isValid("Abcdefg1"));
        assertTrue(UmPasswordValidator.isValid("P@ssword1"));
    }

    @Test
    public void nullOrEmptyPassword_returnsEmpty() {
        assertEquals(UmPasswordValidator.ValidationResult.EMPTY, UmPasswordValidator.validate(null));
        assertEquals(UmPasswordValidator.ValidationResult.EMPTY, UmPasswordValidator.validate(""));

        assertFalse(UmPasswordValidator.isValid(null));
        assertFalse(UmPasswordValidator.isValid(""));
    }

    @Test
    public void tooShortPassword_returnsTooShort() {
        assertEquals(UmPasswordValidator.ValidationResult.TOO_SHORT, UmPasswordValidator.validate("Pass1"));
        assertEquals(UmPasswordValidator.ValidationResult.TOO_SHORT, UmPasswordValidator.validate("1234567"));
        assertEquals(UmPasswordValidator.ValidationResult.TOO_SHORT, UmPasswordValidator.validate("Abc1234"));

        assertFalse(UmPasswordValidator.isValid("Abc1234"));
    }

    @Test
    public void missingUppercase_returnsMissingUppercase() {
        assertEquals(UmPasswordValidator.ValidationResult.MISSING_UPPERCASE, UmPasswordValidator.validate("password123"));
        assertEquals(UmPasswordValidator.ValidationResult.MISSING_UPPERCASE, UmPasswordValidator.validate("abcdefg1"));
        assertEquals(UmPasswordValidator.ValidationResult.MISSING_UPPERCASE, UmPasswordValidator.validate("lowercasenumber1"));

        assertFalse(UmPasswordValidator.isValid("password123"));
    }

    @Test
    public void missingLowercase_returnsMissingLowercase() {
        assertEquals(UmPasswordValidator.ValidationResult.MISSING_LOWERCASE, UmPasswordValidator.validate("PASSWORD123"));
        assertEquals(UmPasswordValidator.ValidationResult.MISSING_LOWERCASE, UmPasswordValidator.validate("ABCDEFG1"));
        assertEquals(UmPasswordValidator.ValidationResult.MISSING_LOWERCASE, UmPasswordValidator.validate("UPPERCASENUM1"));

        assertFalse(UmPasswordValidator.isValid("PASSWORD123"));
    }

    @Test
    public void missingNumber_returnsMissingNumber() {
        assertEquals(UmPasswordValidator.ValidationResult.MISSING_NUMBER, UmPasswordValidator.validate("Password"));
        assertEquals(UmPasswordValidator.ValidationResult.MISSING_NUMBER, UmPasswordValidator.validate("Abcdefgh"));
        assertEquals(UmPasswordValidator.ValidationResult.MISSING_NUMBER, UmPasswordValidator.validate("NoNumbersHere"));

        assertFalse(UmPasswordValidator.isValid("Password"));
    }

    @Test
    public void boundaryEightCharacters_validIfAllRulesMet() {
        // Exactly 8 chars with 1 upper ('A'), 6 lower ('bcdefg'), 1 digit ('1')
        assertEquals(UmPasswordValidator.ValidationResult.VALID, UmPasswordValidator.validate("Abcdefg1"));
        assertTrue(UmPasswordValidator.isValid("Abcdefg1"));
    }

    @Test
    public void passwordWithSpacesAndSymbols_validIfAllRulesMet() {
        // Password with spaces/symbols should retain space and validate correctly
        assertEquals(UmPasswordValidator.ValidationResult.VALID, UmPasswordValidator.validate("P@ss w0rd"));
        assertTrue(UmPasswordValidator.isValid("P@ss w0rd"));
    }
}
