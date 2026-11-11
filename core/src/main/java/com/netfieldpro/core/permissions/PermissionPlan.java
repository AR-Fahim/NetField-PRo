package com.netfieldpro.core.permissions;

public record PermissionPlan(
        PermRow location,
        PermRow notifications,
        boolean surveyReady,
        PrimaryAction primary
) {
}

