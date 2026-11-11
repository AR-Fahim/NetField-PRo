package com.netfieldpro.core.auth;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SignOutPolicyTest {

    @Test
    public void testSO1_inactiveSurveyAllowsSignOut() {
        assertEquals(SignOutDecision.ALLOWED, SignOutPolicy.decide(false));
    }

    @Test
    public void testSO2_activeSurveyBlocksSignOut() {
        assertEquals(SignOutDecision.BLOCKED_ACTIVE_SURVEY, SignOutPolicy.decide(true));
    }
}

