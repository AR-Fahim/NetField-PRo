package com.netfieldpro.core.auth;

public final class SignOutPolicy {

    private SignOutPolicy() {
    }

    public static SignOutDecision decide(boolean surveyActive) {
        return surveyActive ? SignOutDecision.BLOCKED_ACTIVE_SURVEY : SignOutDecision.ALLOWED;
    }
}

