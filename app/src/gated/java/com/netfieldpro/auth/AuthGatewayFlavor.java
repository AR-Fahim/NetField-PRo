package com.netfieldpro.auth;

import android.content.Context;
import android.util.Log;

import com.netfieldpro.BuildConfig;
import com.netfieldpro.auth.experimental.FirebaseAuthGatewayImpl;

public final class AuthGatewayFlavor {

    private AuthGatewayFlavor() {}

    public static AuthSessionGateway create(Context ctx) {
        if (BuildConfig.FB_APP_ID == null || BuildConfig.FB_APP_ID.isEmpty()) {
            Log.i("NF_FACT", "auth NOT_CONFIGURED");
            return new NoOpAuthGatewayImpl();
        }
        return new FirebaseAuthGatewayImpl(ctx != null ? ctx.getApplicationContext() : null);
    }
}

