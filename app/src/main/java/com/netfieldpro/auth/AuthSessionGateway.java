package com.netfieldpro.auth;

import com.netfieldpro.core.auth.AuthOutcome;

public interface AuthSessionGateway {
    boolean hasActiveSession();
    String getCurrentUserEmail();
    void signIn(String email, String pw, AuthCallback cb);
    void register(String email, String pw, AuthCallback cb);
    void sendPasswordReset(String email, AuthCallback cb);
    void signOut();

    default boolean supportsAccounts() {
        return false;
    }

    default String currentEmail() {
        return null;
    }

    default void signIn(String email, String password, Callback cb) {
        if (cb != null) {
            cb.onResult(AuthOutcome.OK);
        }
    }

    default void signUp(String email, String password, Callback cb) {
        if (cb != null) {
            cb.onResult(AuthOutcome.OK);
        }
    }

    default void sendPasswordReset(String email, Callback cb) {
        if (cb != null) {
            cb.onResult(AuthOutcome.OK);
        }
    }

    interface Callback {
        void onResult(AuthOutcome outcome);
    }
}
