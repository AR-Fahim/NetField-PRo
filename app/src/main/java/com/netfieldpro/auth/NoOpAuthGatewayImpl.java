package com.netfieldpro.auth;

public class NoOpAuthGatewayImpl implements AuthSessionGateway {

    @Override
    public boolean hasActiveSession() {
        return true;
    }

    @Override
    public String getCurrentUserEmail() {
        return null;
    }

    @Override
    public void signIn(String email, String pw, AuthCallback cb) {
        if (cb != null) {
            cb.onSuccess();
        }
    }

    @Override
    public void register(String email, String pw, AuthCallback cb) {
        if (cb != null) {
            cb.onSuccess();
        }
    }

    @Override
    public void sendPasswordReset(String email, AuthCallback cb) {
        if (cb != null) {
            cb.onSuccess();
        }
    }

    @Override
    public void signOut() {
    }
}

