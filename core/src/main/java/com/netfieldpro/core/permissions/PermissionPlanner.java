package com.netfieldpro.core.permissions;

import java.util.Objects;

public final class PermissionPlanner {

    public static final int MIN_SUPPORTED_SDK = 30;
    public static final int NOTIFICATION_MIN_SDK = 33;

    private PermissionPlanner() {}

    public static PermissionPlan plan(PermissionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        if (snapshot.sdkInt() < MIN_SUPPORTED_SDK) {
            throw new IllegalArgumentException("SDK " + snapshot.sdkInt() + " is below minimum supported " + MIN_SUPPORTED_SDK);
        }

        PermRow locRow = planLocation(snapshot);
        PermRow notifRow = planNotifications(snapshot);

        boolean surveyReady = (locRow.state() == PermState.GRANTED);

        PrimaryAction primary;
        if (locRow.state() == PermState.GRANTED) {
            primary = PrimaryAction.CONTINUE;
        } else if (locRow.action() == PermAction.REQUEST) {
            primary = PrimaryAction.ALLOW_LOCATION;
        } else {
            primary = PrimaryAction.OPEN_SETTINGS;
        }

        return new PermissionPlan(locRow, notifRow, surveyReady, primary);
    }

    private static PermRow planLocation(PermissionSnapshot s) {
        if (s.fineGranted()) {
            return new PermRow(PermissionKey.LOCATION, PermState.GRANTED, PermAction.NONE);
        }
        if (s.coarseGranted()) {
            return new PermRow(PermissionKey.LOCATION, PermState.APPROXIMATE_ONLY, PermAction.OPEN_SETTINGS);
        }
        return denied(PermissionKey.LOCATION, s.askedLocation(), s.rationaleLocation());
    }

    private static PermRow planNotifications(PermissionSnapshot s) {
        if (s.sdkInt() < NOTIFICATION_MIN_SDK) {
            return new PermRow(PermissionKey.NOTIFICATIONS, PermState.NOT_REQUIRED, PermAction.NONE);
        }
        if (s.notifGranted()) {
            return new PermRow(PermissionKey.NOTIFICATIONS, PermState.GRANTED, PermAction.NONE);
        }
        return denied(PermissionKey.NOTIFICATIONS, s.askedNotif(), s.rationaleNotif());
    }

    private static PermRow denied(PermissionKey key, boolean asked, boolean rationale) {
        if (!asked || rationale) {
            return new PermRow(key, PermState.DENIED_CAN_ASK, PermAction.REQUEST);
        }
        return new PermRow(key, PermState.DENIED_PERMANENT, PermAction.OPEN_SETTINGS);
    }
}

