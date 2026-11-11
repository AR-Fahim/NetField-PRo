package com.netfieldpro.core.auth;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AuthErrorMapperTest {

    @Test
    public void testAE1_allMappedCodes() {
        assertEquals(AuthOutcome.INVALID_EMAIL, AuthErrorMapper.map("ERROR_INVALID_EMAIL"));
        assertEquals(AuthOutcome.INVALID_CREDENTIALS, AuthErrorMapper.map("ERROR_WRONG_PASSWORD"));
        assertEquals(AuthOutcome.INVALID_CREDENTIALS, AuthErrorMapper.map("ERROR_INVALID_CREDENTIAL"));
        assertEquals(AuthOutcome.INVALID_CREDENTIALS, AuthErrorMapper.map("ERROR_INVALID_LOGIN_CREDENTIALS"));
        assertEquals(AuthOutcome.INVALID_CREDENTIALS, AuthErrorMapper.map("ERROR_USER_NOT_FOUND"));
        assertEquals(AuthOutcome.EMAIL_IN_USE, AuthErrorMapper.map("ERROR_EMAIL_ALREADY_IN_USE"));
        assertEquals(AuthOutcome.WEAK_PASSWORD, AuthErrorMapper.map("ERROR_WEAK_PASSWORD"));
        assertEquals(AuthOutcome.TOO_MANY_REQUESTS, AuthErrorMapper.map("ERROR_TOO_MANY_REQUESTS"));
        assertEquals(AuthOutcome.NO_NETWORK, AuthErrorMapper.map("ERROR_NETWORK_REQUEST_FAILED"));
        assertEquals(AuthOutcome.PROVIDER_DISABLED, AuthErrorMapper.map("ERROR_OPERATION_NOT_ALLOWED"));
    }

    @Test
    public void testAE2_unrecognizedCode() {
        assertEquals(AuthOutcome.UNKNOWN, AuthErrorMapper.map("ERROR_SOMETHING"));
    }

    @Test
    public void testAE3_nullCode() {
        assertEquals(AuthOutcome.UNKNOWN, AuthErrorMapper.map(null));
    }
}

