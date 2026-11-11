package com.netfieldpro.auth;

public interface AuthCallback {
    void onSuccess();
    void onError(AuthError error);
}

