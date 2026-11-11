package com.netfieldpro;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.netfieldpro.core.model.entity.Project;
import com.netfieldpro.core.settings.HeatPalette;
import com.netfieldpro.core.settings.ThemeMode;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.repository.ProjectRepository;
import com.netfieldpro.entitlement.PrefsTierSource;
import com.netfieldpro.settings.AppSettings;

public final class TestSetup {
    private TestSetup() {}

    public static void skipOnboarding() {
        NetFieldApp.onboardingStore().markDone();
    }

    public static void resetAppState() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                executor.submit(TestSetup::resetAppStateInternal).get();
            } catch (Exception e) {
                Throwable cause = (e instanceof ExecutionException && e.getCause() != null) ? e.getCause() : e;
                if (cause instanceof AssertionError) {
                    throw (AssertionError) cause;
                }
                throw new RuntimeException("resetAppState failed", cause);
            } finally {
                executor.shutdown();
            }
        } else {
            resetAppStateInternal();
        }
    }

    private static void resetAppStateInternal() {
        Context context = ApplicationProvider.getApplicationContext();
        NetFieldDatabase db = NetFieldDatabase.get(context);
        ProjectRepository repo = NetFieldApp.projectRepository();

        // 1. Off the main thread, list all projects and delete each via ProjectRepository.delete,
        // waiting on the completion signal (no sleep).
        List<Project> projects = repo.getProjects();
        if (projects != null) {
            for (Project p : projects) {
                repo.delete(p.id());
            }
        }

        // 2. Delete all files under filesDir/plans (create dir if absent).
        File plansDir = new File(context.getFilesDir(), "plans");
        if (!plansDir.exists()) {
            plansDir.mkdirs();
        } else {
            File[] files = plansDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }

        // 3. Reset prefs nf_settings to defaults (theme SYSTEM, palette TRAFFIC_LIGHT).
        SharedPreferences settingsPrefs = context.getSharedPreferences(AppSettings.PREF_NAME, Context.MODE_PRIVATE);
        settingsPrefs.edit()
                .putString(AppSettings.KEY_THEME_MODE, ThemeMode.SYSTEM.name())
                .putString(AppSettings.KEY_HEAT_PALETTE, HeatPalette.TRAFFIC_LIGHT.name())
                .commit();

        // 4. Clear the debug entitlement override so tier = FREE.
        SharedPreferences entPrefs = context.getSharedPreferences(PrefsTierSource.PREF_NAME, Context.MODE_PRIVATE);
        entPrefs.edit().remove(PrefsTierSource.KEY_DEBUG_TIER).commit();
        try {
            NetFieldApp.entitlements().setDebugTier(null);
        } catch (Throwable ignored) {
        }

        // 5. Verify: live COUNT of projects == 0 and plans dir has 0 files. Otherwise throw
        // AssertionError("RESET_FAILED projects=<n> files=<m>").
        int projectCount = db.netFieldDao().countProjects();
        File[] remainingFiles = plansDir.listFiles();
        int fileCount = (remainingFiles != null) ? remainingFiles.length : 0;
        if (projectCount != 0 || fileCount != 0) {
            throw new AssertionError("RESET_FAILED projects=" + projectCount + " files=" + fileCount);
        }

        // 6. Verify the 5 seeded wall materials still exist (count == 5), else
        // AssertionError("RESET_BROKE_SEED").
        int wallCount = db.netFieldDao().countWallMaterials();
        if (wallCount != 5) {
            throw new AssertionError("RESET_BROKE_SEED");
        }
    }
}
