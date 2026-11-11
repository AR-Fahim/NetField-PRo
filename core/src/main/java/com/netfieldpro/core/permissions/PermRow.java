package com.netfieldpro.core.permissions;

public record PermRow(
        PermissionKey key,
        PermState state,
        PermAction action
) {
    @Override
    public String toString() {
        return key.name() + ":" + state.name() + ":" + action.name();
    }
}

