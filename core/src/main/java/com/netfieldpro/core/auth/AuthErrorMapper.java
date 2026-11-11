package com.netfieldpro.core.auth;

public final class AuthErrorMapper {

    private AuthErrorMapper() {
    }

    public static AuthOutcome map(String code) {
        if (code == null) {
            return AuthOutcome.UNKNOWN;
        }
        return switch (code) {
            case "ERROR_INVALID_EMAIL" -> AuthOutcome.INVALID_EMAIL;
            case "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL",
                 "ERROR_INVALID_LOGIN_CREDENTIALS", "ERROR_USER_NOT_FOUND" -> AuthOutcome.INVALID_CREDENTIALS;
            case "ERROR_EMAIL_ALREADY_IN_USE" -> AuthOutcome.EMAIL_IN_USE;
            case "ERROR_WEAK_PASSWORD" -> AuthOutcome.WEAK_PASSWORD;
            case "ERROR_TOO_MANY_REQUESTS" -> AuthOutcome.TOO_MANY_REQUESTS;
            case "ERROR_NETWORK_REQUEST_FAILED" -> AuthOutcome.NO_NETWORK;
            case "ERROR_OPERATION_NOT_ALLOWED" -> AuthOutcome.PROVIDER_DISABLED;
            default -> AuthOutcome.UNKNOWN;
        };
    }
}

