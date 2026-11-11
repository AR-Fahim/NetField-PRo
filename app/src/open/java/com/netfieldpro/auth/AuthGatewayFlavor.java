package com.netfieldpro.auth;

import android.content.Context;

public final class AuthGatewayFlavor {

    private AuthGatewayFlavor() {}

    public static AuthSessionGateway create(Context ctx) {
        return new NoOpAuthGatewayImpl();
    }
}

