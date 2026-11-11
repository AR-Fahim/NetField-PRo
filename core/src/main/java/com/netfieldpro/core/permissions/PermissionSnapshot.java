package com.netfieldpro.core.permissions;

public record PermissionSnapshot(
        int sdkInt,
        boolean fineGranted,
        boolean coarseGranted,
        boolean askedLocation,
        boolean rationaleLocation,
        boolean notifGranted,
        boolean askedNotif,
        boolean rationaleNotif
) {
}

