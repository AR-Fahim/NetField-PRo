package com.netfieldpro.ui.placeholder;

@FunctionalInterface
public interface PermissionRequestHook {
    void onRequest(String[] permissions);
}

