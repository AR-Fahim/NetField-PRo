package com.netfieldpro.core.auth;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AuthInputValidatorTest {

    @Test
    public void testAU1_validSignIn() {
        assertEquals(InputCheck.OK, AuthInputValidator.signIn("a@b.co", "secret1"));
    }

    @Test
    public void testAU2_emptyEmail() {
        assertEquals(InputCheck.EMAIL_EMPTY, AuthInputValidator.signIn("", "secret1"));
        assertEquals(InputCheck.EMAIL_EMPTY, AuthInputValidator.signIn("   ", "secret1"));
        assertEquals(InputCheck.EMAIL_EMPTY, AuthInputValidator.signUp("", "secret1"));
        assertEquals(InputCheck.EMAIL_EMPTY, AuthInputValidator.signUp("   ", "secret1"));
        assertEquals(InputCheck.EMAIL_EMPTY, AuthInputValidator.reset(""));
        assertEquals(InputCheck.EMAIL_EMPTY, AuthInputValidator.reset("   "));
    }

    @Test
    public void testAU3_invalidEmailVariants() {
        String[] invalids = {"a@b", "a@@b.co", "a b@c.co", "@b.co", "a@b.", "a@.co"};
        for (String email : invalids) {
            assertEquals("Should be EMAIL_INVALID: " + email,
                    InputCheck.EMAIL_INVALID, AuthInputValidator.signIn(email, "secret1"));
            assertEquals("Should be EMAIL_INVALID: " + email,
                    InputCheck.EMAIL_INVALID, AuthInputValidator.signUp(email, "secret1"));
            assertEquals("Should be EMAIL_INVALID: " + email,
                    InputCheck.EMAIL_INVALID, AuthInputValidator.reset(email));
        }
    }

    @Test
    public void testAU4_signUpPasswordLength() {
        assertEquals(InputCheck.PASSWORD_TOO_SHORT, AuthInputValidator.signUp("a@b.co", "12345"));
        assertEquals(InputCheck.OK, AuthInputValidator.signUp("a@b.co", "123456"));

        String pw128 = "a".repeat(128);
        assertEquals(InputCheck.OK, AuthInputValidator.signUp("a@b.co", pw128));

        String pw129 = "a".repeat(129);
        assertEquals(InputCheck.PASSWORD_TOO_LONG, AuthInputValidator.signUp("a@b.co", pw129));
    }

    @Test
    public void testAU5_signInPassword() {
        assertEquals(InputCheck.PASSWORD_EMPTY, AuthInputValidator.signIn("a@b.co", ""));
        assertEquals(InputCheck.PASSWORD_EMPTY, AuthInputValidator.signIn("a@b.co", null));
        assertEquals(InputCheck.OK, AuthInputValidator.signIn("a@b.co", "x"));
    }

    @Test
    public void testAU6_trimmedEmail() {
        assertEquals(InputCheck.OK, AuthInputValidator.signIn(" a@b.co ", "secret1"));
        assertEquals("A@B.co", AuthInputValidator.normalizeEmail(" A@B.co "));
    }

    @Test
    public void testAU7_emailLengthOver254() {
        String longEmail = "a".repeat(250) + "@b.co"; // 255 chars
        assertEquals(255, longEmail.length());
        assertEquals(InputCheck.EMAIL_INVALID, AuthInputValidator.signIn(longEmail, "secret1"));
    }
}

