package com.netfieldpro.core.wifi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EnvironmentAdvisor {
    private EnvironmentAdvisor() {}

    public enum Issue {
        PERMISSION_MISSING,
        LOCATION_SERVICES_OFF,
        WIFI_OFF,
        THROTTLE_ON,
        THROTTLE_UNKNOWN
    }

    public enum ThrottleState {
        ON,
        OFF,
        UNKNOWN
    }

    public record Advice(List<Issue> issues, boolean isReady) {}

    public static Advice advise(boolean locationPermission, boolean locationServices,
                               boolean wifiEnabled, ThrottleState throttleState) {
        List<Issue> issues = new ArrayList<>();
        if (!locationPermission) {
            issues.add(Issue.PERMISSION_MISSING);
        }
        if (!locationServices) {
            issues.add(Issue.LOCATION_SERVICES_OFF);
        }
        if (!wifiEnabled) {
            issues.add(Issue.WIFI_OFF);
        }
        if (throttleState == ThrottleState.ON) {
            issues.add(Issue.THROTTLE_ON);
        } else if (throttleState == ThrottleState.UNKNOWN) {
            issues.add(Issue.THROTTLE_UNKNOWN);
        }

        boolean ready = locationPermission && locationServices && wifiEnabled;
        return new Advice(Collections.unmodifiableList(issues), ready);
    }
}
