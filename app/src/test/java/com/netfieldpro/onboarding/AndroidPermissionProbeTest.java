package com.netfieldpro.onboarding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.core.permissions.PermissionKey;
import com.netfieldpro.core.permissions.PermissionSnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AndroidPermissionProbeTest {

    private OnboardingStore store;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        prefs = context.getSharedPreferences(OnboardingStore.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        store = new OnboardingStore(prefs, true);
    }

    @Test
    public void testSnapshotReadsStoreAndSdk() {
        AndroidPermissionProbe probe = new AndroidPermissionProbe(store);
        Activity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        PermissionSnapshot s1 = probe.snapshot(activity);
        assertNotNull(s1);
        assertEquals(Build.VERSION.SDK_INT, s1.sdkInt());
        assertFalse(s1.askedLocation());
        assertFalse(s1.askedNotif());

        store.markAsked(PermissionKey.LOCATION);
        store.markAsked(PermissionKey.NOTIFICATIONS);

        PermissionSnapshot s2 = probe.snapshot(activity);
        assertTrue(s2.askedLocation());
        assertTrue(s2.askedNotif());
    }

    @Test
    public void testNetFieldAppProbeSeam() {
        PermissionProbe customProbe = a -> new PermissionSnapshot(34, true, true, true, false, true, true, false);
        NetFieldApp.setPermissionProbeForTest(customProbe);
        assertEquals(customProbe, NetFieldApp.permissionProbe());

        NetFieldApp.setPermissionProbeForTest(null);
    }
}

