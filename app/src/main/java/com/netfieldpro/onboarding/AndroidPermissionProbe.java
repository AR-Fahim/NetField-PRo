package com.netfieldpro.onboarding;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.netfieldpro.core.permissions.PermissionKey;
import com.netfieldpro.core.permissions.PermissionSnapshot;

public class AndroidPermissionProbe implements PermissionProbe {

    private final OnboardingStore store;

    public AndroidPermissionProbe(OnboardingStore store) {
        this.store = store;
    }

    @Override
    public PermissionSnapshot snapshot(Activity a) {
        int sdkInt = Build.VERSION.SDK_INT;

        boolean fineGranted = ContextCompat.checkSelfPermission(a, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(a, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean notifGranted = ContextCompat.checkSelfPermission(a, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;

        boolean askedLocation = store.wasAsked(PermissionKey.LOCATION);
        boolean askedNotif = store.wasAsked(PermissionKey.NOTIFICATIONS);

        boolean rationaleLocation = ActivityCompat.shouldShowRequestPermissionRationale(
                a, Manifest.permission.ACCESS_FINE_LOCATION);
        boolean rationaleNotif = ActivityCompat.shouldShowRequestPermissionRationale(
                a, Manifest.permission.POST_NOTIFICATIONS);

        return new PermissionSnapshot(
                sdkInt,
                fineGranted,
                coarseGranted,
                askedLocation,
                rationaleLocation,
                notifGranted,
                askedNotif,
                rationaleNotif
        );
    }
}

