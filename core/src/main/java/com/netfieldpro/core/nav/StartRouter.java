package com.netfieldpro.core.nav;

public final class StartRouter {

    private StartRouter() {}

    public enum StartIntent {
        MAIN,
        VIEW_OR_SEND_DOCUMENT
    }

    public enum StartDestination {
        S00,
        S01,
        S02,
        S17
    }

    public static StartDestination route(StartIntent intent, boolean hasActiveSession, boolean firstLaunchDone) {
        if (intent == StartIntent.VIEW_OR_SEND_DOCUMENT) {
            return StartDestination.S17;
        }
        if (!hasActiveSession) {
            return StartDestination.S00;
        }
        if (!firstLaunchDone) {
            return StartDestination.S01;
        }
        return StartDestination.S02;
    }
}

