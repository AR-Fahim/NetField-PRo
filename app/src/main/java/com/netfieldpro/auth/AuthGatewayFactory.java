package com.netfieldpro.auth;

import android.content.Context;
import androidx.annotation.VisibleForTesting;
import com.netfieldpro.NetFieldApp;

public final class AuthGatewayFactory {

    private static AuthSessionGateway cachedGateway;
    private static AuthSessionGateway testOverride;

    private AuthGatewayFactory() {}

    public static synchronized AuthSessionGateway create() {
        return create(NetFieldApp.get());
    }

    public static synchronized AuthSessionGateway create(Context ctx) {
        if (testOverride != null) {
            return testOverride;
        }
        if (cachedGateway == null) {
            cachedGateway = AuthGatewayFlavor.create(ctx != null ? ctx.getApplicationContext() : null);
        }
        return cachedGateway;
    }

    @VisibleForTesting
    public static synchronized void setForTesting(AuthSessionGateway gw) {
        testOverride = gw;
    }

    @VisibleForTesting
    public static synchronized void resetForTesting() {
        testOverride = null;
        cachedGateway = null;
    }
}
