package com.netfieldpro.onboarding;

import android.app.Activity;

import com.netfieldpro.core.permissions.PermissionSnapshot;

public interface PermissionProbe {
    PermissionSnapshot snapshot(Activity a);
}

