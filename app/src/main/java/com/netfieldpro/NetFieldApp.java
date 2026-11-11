package com.netfieldpro;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.netfieldpro.core.settings.ThemeMode;
import com.netfieldpro.entitlement.EntitlementRepository;
import com.netfieldpro.entitlement.PrefsTierSource;
import com.netfieldpro.onboarding.AndroidPermissionProbe;
import com.netfieldpro.onboarding.OnboardingStore;
import com.netfieldpro.onboarding.PermissionProbe;
import com.netfieldpro.settings.AppSettings;

public class NetFieldApp extends Application {

    private static NetFieldApp instance;
    private EntitlementRepository entitlements;
    private OnboardingStore onboardingStore;
    private PermissionProbe permissionProbe;
    private com.netfieldpro.integrity.FinalizeCoordinator finalizeCoordinator;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        AppSettings settings = new AppSettings(this);
        ThemeMode mode = settings.getThemeMode();
        int nightMode = switch (mode) {
            case LIGHT -> AppCompatDelegate.MODE_NIGHT_NO;
            case DARK -> AppCompatDelegate.MODE_NIGHT_YES;
            case SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        };
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    public static NetFieldApp get() {
        return instance;
    }

    public static EntitlementRepository entitlements() {
        return instance.getEntitlements();
    }

    public synchronized EntitlementRepository getEntitlements() {
        if (entitlements == null) {
            PrefsTierSource tierSource = new PrefsTierSource(
                    getSharedPreferences(PrefsTierSource.PREF_NAME, MODE_PRIVATE),
                    BuildConfig.DEBUG
            );
            entitlements = new EntitlementRepository(tierSource);
        }
        return entitlements;
    }

    public static com.netfieldpro.onboarding.OnboardingStore onboardingStore() {
        return instance.getOnboardingStore();
    }

    public synchronized OnboardingStore getOnboardingStore() {
        if (onboardingStore == null) {
            onboardingStore = new OnboardingStore(
                    getSharedPreferences(OnboardingStore.PREFS_NAME, MODE_PRIVATE),
                    BuildConfig.DEBUG
            );
        }
        return onboardingStore;
    }

    private com.netfieldpro.core.wifi.WifiScanSource wifiScanSource;
    private com.netfieldpro.core.sensor.SensorSource sensorSource;
    private com.netfieldpro.wifi.WifiEnvironmentProbe wifiEnvironmentProbe;

    public static PermissionProbe permissionProbe() {
        return instance.getPermissionProbe();
    }

    public synchronized PermissionProbe getPermissionProbe() {
        if (permissionProbe == null) {
            permissionProbe = new AndroidPermissionProbe(getOnboardingStore());
        }
        return permissionProbe;
    }

    @androidx.annotation.VisibleForTesting
    public static void setPermissionProbeForTest(PermissionProbe probe) {
        if (instance != null) {
            instance.permissionProbe = probe;
        }
    }

    public static com.netfieldpro.core.wifi.WifiScanSource wifiScanSource() {
        return instance.getWifiScanSource();
    }

    public synchronized com.netfieldpro.core.wifi.WifiScanSource getWifiScanSource() {
        if (wifiScanSource == null) {
            wifiScanSource = new com.netfieldpro.wifi.AndroidWifiScanSource(this, new com.netfieldpro.time.AndroidClock());
        }
        return wifiScanSource;
    }

    @androidx.annotation.VisibleForTesting
    public static void setWifiScanSourceForTest(com.netfieldpro.core.wifi.WifiScanSource source) {
        if (instance != null) {
            instance.wifiScanSource = source;
        }
    }

    public static com.netfieldpro.core.sensor.SensorSource sensorSource() {
        return instance != null ? instance.getSensorSource() : null;
    }

    public synchronized com.netfieldpro.core.sensor.SensorSource getSensorSource() {
        if (sensorSource == null) {
            sensorSource = new com.netfieldpro.sensor.AndroidSensorSource(this);
        }
        return sensorSource;
    }

    @androidx.annotation.VisibleForTesting
    public static void setSensorSourceForTest(com.netfieldpro.core.sensor.SensorSource source) {
        if (instance != null) {
            instance.sensorSource = source;
        }
    }

    public static com.netfieldpro.wifi.WifiEnvironmentProbe wifiEnvironmentProbe() {
        return instance.getWifiEnvironmentProbe();
    }

    public synchronized com.netfieldpro.wifi.WifiEnvironmentProbe getWifiEnvironmentProbe() {
        if (wifiEnvironmentProbe == null) {
            wifiEnvironmentProbe = new com.netfieldpro.wifi.WifiEnvironmentProbe(this);
        }
        return wifiEnvironmentProbe;
    }

    @androidx.annotation.VisibleForTesting
    public static void setWifiEnvironmentProbeForTest(com.netfieldpro.wifi.WifiEnvironmentProbe probe) {
        if (instance != null) {
            instance.wifiEnvironmentProbe = probe;
        }
    }

    private com.netfieldpro.latency.LatencyRunner latencyRunner;
    private com.netfieldpro.latency.WifiNetworkTracker wifiNetworkTracker;

    public static com.netfieldpro.latency.LatencyRunner latencyRunner() {
        return instance.getLatencyRunner();
    }

    public synchronized com.netfieldpro.latency.LatencyRunner getLatencyRunner() {
        if (latencyRunner == null) {
            latencyRunner = new com.netfieldpro.latency.DefaultLatencyRunner();
        }
        return latencyRunner;
    }

    @androidx.annotation.VisibleForTesting
    public static void setLatencyRunnerForTest(com.netfieldpro.latency.LatencyRunner runner) {
        if (instance != null) {
            instance.latencyRunner = runner;
        }
    }

    public static com.netfieldpro.latency.WifiNetworkTracker wifiNetworkTracker() {
        return instance.getWifiNetworkTracker();
    }

    public synchronized com.netfieldpro.latency.WifiNetworkTracker getWifiNetworkTracker() {
        if (wifiNetworkTracker == null) {
            wifiNetworkTracker = new com.netfieldpro.latency.WifiNetworkTracker(this);
        }
        return wifiNetworkTracker;
    }

    @androidx.annotation.VisibleForTesting
    public static void setWifiNetworkTrackerForTest(com.netfieldpro.latency.WifiNetworkTracker tracker) {
        if (instance != null) {
            instance.wifiNetworkTracker = tracker;
        }
    }

    private com.netfieldpro.data.repository.PlanFileStore planFileStore;
    private com.netfieldpro.data.repository.QuotaGuard quotaGuard;
    private com.netfieldpro.data.repository.ProjectRepository projectRepository;
    private com.netfieldpro.data.repository.FloorRepository floorRepository;
    private com.netfieldpro.data.repository.SurveyReadRepository surveyReadRepository;

    public static com.netfieldpro.data.repository.ProjectRepository projectRepository() {
        return instance.getProjectRepository();
    }

    public synchronized com.netfieldpro.data.repository.ProjectRepository getProjectRepository() {
        if (projectRepository == null) {
            projectRepository = com.netfieldpro.data.repository.ProjectRepository.create(this, getEntitlements());
        }
        return projectRepository;
    }

    @androidx.annotation.VisibleForTesting
    public static void setProjectRepositoryForTest(com.netfieldpro.data.repository.ProjectRepository repo) {
        if (instance != null) {
            instance.projectRepository = repo;
        }
    }

    public static com.netfieldpro.data.repository.FloorRepository floorRepository() {
        return instance.getFloorRepository();
    }

    public synchronized com.netfieldpro.data.repository.FloorRepository getFloorRepository() {
        if (floorRepository == null) {
            floorRepository = com.netfieldpro.data.repository.FloorRepository.create(this, getEntitlements());
        }
        return floorRepository;
    }

    @androidx.annotation.VisibleForTesting
    public static void setFloorRepositoryForTest(com.netfieldpro.data.repository.FloorRepository repo) {
        if (instance != null) {
            instance.floorRepository = repo;
        }
    }

    public static com.netfieldpro.data.repository.PlanFileStore planFileStore() {
        return instance.getPlanFileStore();
    }

    public synchronized com.netfieldpro.data.repository.PlanFileStore getPlanFileStore() {
        if (planFileStore == null) {
            planFileStore = new com.netfieldpro.data.repository.PlanFileStore(this);
        }
        return planFileStore;
    }

    @androidx.annotation.VisibleForTesting
    public static void setPlanFileStoreForTest(com.netfieldpro.data.repository.PlanFileStore store) {
        if (instance != null) {
            instance.planFileStore = store;
        }
    }

    public static com.netfieldpro.data.repository.QuotaGuard quotaGuard() {
        return instance.getQuotaGuard();
    }

    public synchronized com.netfieldpro.data.repository.QuotaGuard getQuotaGuard() {
        if (quotaGuard == null) {
            quotaGuard = com.netfieldpro.data.repository.QuotaGuard.create(this, getEntitlements());
        }
        return quotaGuard;
    }

    @androidx.annotation.VisibleForTesting
    public static void setQuotaGuardForTest(com.netfieldpro.data.repository.QuotaGuard guard) {
        if (instance != null) {
            instance.quotaGuard = guard;
        }
    }

    public static com.netfieldpro.data.repository.SurveyReadRepository surveyReadRepository() {
        return instance.getSurveyReadRepository();
    }

    public synchronized com.netfieldpro.data.repository.SurveyReadRepository getSurveyReadRepository() {
        if (surveyReadRepository == null) {
            surveyReadRepository = com.netfieldpro.data.repository.SurveyReadRepository.create(this);
        }
        return surveyReadRepository;
    }

    @androidx.annotation.VisibleForTesting
    public static void setSurveyReadRepositoryForTest(com.netfieldpro.data.repository.SurveyReadRepository repo) {
        if (instance != null) {
            instance.surveyReadRepository = repo;
        }
    }

    public static com.netfieldpro.integrity.FinalizeCoordinator finalizeCoordinator() {
        return instance.getFinalizeCoordinator();
    }

    public synchronized com.netfieldpro.integrity.FinalizeCoordinator getFinalizeCoordinator() {
        if (finalizeCoordinator == null) {
            finalizeCoordinator = com.netfieldpro.integrity.FinalizeCoordinator.create(this);
        }
        return finalizeCoordinator;
    }

    @androidx.annotation.VisibleForTesting
    public static void setFinalizeCoordinatorForTest(com.netfieldpro.integrity.FinalizeCoordinator coordinator) {
        if (instance != null) {
            instance.finalizeCoordinator = coordinator;
        }
    }

    private com.netfieldpro.session.SessionExecutor sessionExecutor;

    public static com.netfieldpro.session.SessionExecutor sessionExecutor() {
        return instance != null ? instance.getSessionExecutor() : null;
    }

    public synchronized com.netfieldpro.session.SessionExecutor getSessionExecutor() {
        if (sessionExecutor == null) {
            sessionExecutor = com.netfieldpro.session.SessionExecutor.create(this);
        }
        return sessionExecutor;
    }

    @androidx.annotation.VisibleForTesting
    public static void setSessionExecutorForTest(com.netfieldpro.session.SessionExecutor executor) {
        if (instance != null) {
            instance.sessionExecutor = executor;
        }
    }

    private com.netfieldpro.power.BatteryAlertMonitor batteryAlertMonitor;
    private static volatile com.netfieldpro.power.BatteryAlertMonitor fallbackBatteryAlertMonitor;

    public static com.netfieldpro.power.BatteryAlertMonitor batteryAlerts() {
        if (instance != null) {
            return instance.getBatteryAlertMonitor();
        }
        if (fallbackBatteryAlertMonitor == null) {
            synchronized (NetFieldApp.class) {
                if (fallbackBatteryAlertMonitor == null) {
                    fallbackBatteryAlertMonitor = new com.netfieldpro.power.BatteryAlertMonitor();
                }
            }
        }
        return fallbackBatteryAlertMonitor;
    }

    public synchronized com.netfieldpro.power.BatteryAlertMonitor getBatteryAlertMonitor() {
        if (batteryAlertMonitor == null) {
            batteryAlertMonitor = new com.netfieldpro.power.BatteryAlertMonitor();
        }
        return batteryAlertMonitor;
    }

    @androidx.annotation.VisibleForTesting
    public static void setBatteryAlertMonitorForTest(com.netfieldpro.power.BatteryAlertMonitor monitor) {
        if (instance != null) {
            instance.batteryAlertMonitor = monitor;
        } else {
            fallbackBatteryAlertMonitor = monitor;
        }
    }
}
