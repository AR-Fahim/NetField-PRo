package com.netfieldpro.core.auth;

public enum AuthOutcome {
    OK,
    INVALID_CREDENTIALS,
    EMAIL_IN_USE,
    WEAK_PASSWORD,
    INVALID_EMAIL,
    NO_NETWORK,
    TOO_MANY_REQUESTS,
    PROVIDER_DISABLED,
    UNKNOWN
}

