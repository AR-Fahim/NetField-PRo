package com.netfieldpro.ui.survey;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.DecisionKind;
import com.netfieldpro.core.entitlement.EntitlementDecision;
import com.netfieldpro.core.entitlement.EntitlementPolicy;
import com.netfieldpro.core.entitlement.Feature;
import com.netfieldpro.core.entitlement.QuotaDecision;
import com.netfieldpro.core.entitlement.Resource;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.core.latency.ProbeCandidate;
import com.netfieldpro.core.latency.ProbeMethodSelector;
import com.netfieldpro.core.latency.ProbePlanner;
import com.netfieldpro.core.model.entity.RequirementProfile;
import com.netfieldpro.core.model.entity.SurveySession;
import com.netfieldpro.core.model.enums.ProbeMethod;
import com.netfieldpro.core.permissions.PermAction;
import com.netfieldpro.core.permissions.PermissionPlan;
import com.netfieldpro.core.permissions.PermissionPlanner;
import com.netfieldpro.core.permissions.PermissionSnapshot;
import com.netfieldpro.core.project.NameRules;
import com.netfieldpro.core.time.Clock;
import com.netfieldpro.core.verdict.PresetProfiles;
import com.netfieldpro.core.verdict.ProfileSpec;
import com.netfieldpro.core.wifi.EnvironmentAdvisor;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.repository.SessionRepository;
import com.netfieldpro.latency.DgramIcmpProbe;
import com.netfieldpro.latency.TcpConnectProbe;
import com.netfieldpro.latency.WifiNetworkTracker;
import com.netfieldpro.onboarding.PermissionProbe;
import com.netfieldpro.time.AndroidClock;
import com.netfieldpro.ui.NavArgs;
import com.netfieldpro.wifi.WifiEnvironmentProbe;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class NewSurveyFragment extends Fragment {

    private String floorId;

    // Views - Common
    private TextView textScreenTitle;
    private TextView textStepIndicator;

    // Views - Step 1
    private View layoutStep1;
    private TextView textEnvPermission;
    private TextView textEnvServices;
    private TextView textEnvWifi;
    private TextView textEnvThrottle;
    private MaterialButton btnDevOptions;
    private MaterialCardView cardEnvIssue;
    private TextView textEnvIssue;
    private MaterialButton btnEnvFix;
    private MaterialButton btnNext;

    // Views - Step 2
    private View layoutStep2;
    private TextView textWarmupStatus;
    private ProgressBar progressWarmup;
    private TextView textNetworksTitle;
    private RecyclerView recyclerNetworks;
    private MaterialCardView cardWarmupFailed;
    private TextView textWarmupFailed;
    private MaterialButton btnWarmupRetry;
    private MaterialButton btnWarmupContinueIndicative;
    private MaterialButton btnStep2Next;
    private WarmupNetworksAdapter networksAdapter;

    // Views - Step 3
    private View layoutStep3;
    private TextInputLayout inputLayoutSurveyName;
    private TextInputEditText editSurveyName;
    private TextView textProfileLabel;
    private Spinner spinnerProfile;
    private TextView textLatencyMethod;
    private TextView textIndicativeWarning;
    private MaterialButton btnStartSurvey;

    // Views - Walk edges
    private MaterialButton btnWalkS18;
    private MaterialButton btnWalkS07;

    private ActivityResultLauncher<String[]> permissionLauncher;
    private WarmupCoordinator warmupCoordinator;
    private Clock clock = new AndroidClock();
    private SessionRepository sessionRepo;

    private List<ProfileSpec> presetList = Collections.emptyList();
    private String selectedProfileId = PresetProfiles.idFor(PresetProfiles.OFFICE_STANDARD.key());
    private int lastValidProfileIndex = 0;
    private String selectedProbeMethod = "NONE";
    private boolean spinnerInitialized = false;
    private boolean isProgrammaticSelection = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            floorId = getArguments().getString(NavArgs.FLOOR_ID);
        }
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> refreshEnvironment()
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_survey, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textScreenTitle = view.findViewById(R.id.text_screen_title);
        textStepIndicator = view.findViewById(R.id.text_step_indicator);

        layoutStep1 = view.findViewById(R.id.layout_step1);
        textEnvPermission = view.findViewById(R.id.text_env_permission);
        textEnvServices = view.findViewById(R.id.text_env_services);
        textEnvWifi = view.findViewById(R.id.text_env_wifi);
        textEnvThrottle = view.findViewById(R.id.text_env_throttle);
        btnDevOptions = view.findViewById(R.id.btn_dev_options);
        cardEnvIssue = view.findViewById(R.id.card_env_issue);
        textEnvIssue = view.findViewById(R.id.text_env_issue);
        btnEnvFix = view.findViewById(R.id.btn_env_fix);
        btnNext = view.findViewById(R.id.btn_next);

        layoutStep2 = view.findViewById(R.id.layout_step2);
        textWarmupStatus = view.findViewById(R.id.text_warmup_status);
        progressWarmup = view.findViewById(R.id.progress_warmup);
        textNetworksTitle = view.findViewById(R.id.text_networks_title);
        recyclerNetworks = view.findViewById(R.id.recycler_networks);
        cardWarmupFailed = view.findViewById(R.id.card_warmup_failed);
        textWarmupFailed = view.findViewById(R.id.text_warmup_failed);
        btnWarmupRetry = view.findViewById(R.id.btn_warmup_retry);
        btnWarmupContinueIndicative = view.findViewById(R.id.btn_warmup_continue_indicative);
        btnStep2Next = view.findViewById(R.id.btn_step2_next);

        layoutStep3 = view.findViewById(R.id.layout_step3);
        inputLayoutSurveyName = view.findViewById(R.id.input_layout_survey_name);
        editSurveyName = view.findViewById(R.id.edit_survey_name);
        textProfileLabel = view.findViewById(R.id.text_profile_label);
        spinnerProfile = view.findViewById(R.id.spinner_profile);
        textLatencyMethod = view.findViewById(R.id.text_latency_method);
        textIndicativeWarning = view.findViewById(R.id.text_indicative_warning);
        btnStartSurvey = view.findViewById(R.id.btn_start_survey);

        btnWalkS18 = view.findViewById(R.id.btn_walk_s18);
        btnWalkS07 = view.findViewById(R.id.btn_walk_s07);

        btnDevOptions.setOnClickListener(v -> openDeveloperOptions());
        btnNext.setOnClickListener(v -> moveToStep2());

        if (btnWalkS18 != null) {
            btnWalkS18.setOnClickListener(v -> {
                NavHostFragment.findNavController(this).navigate(R.id.action_s06_to_s18);
            });
        }
        if (btnWalkS07 != null) {
            btnWalkS07.setOnClickListener(v -> {
                NavHostFragment.findNavController(this).navigate(R.id.action_s06_to_s07);
            });
        }

        setupStep2Views();
        setupStep3Views();
    }

    private void setupStep2Views() {
        networksAdapter = new WarmupNetworksAdapter(item -> {
            if (warmupCoordinator != null) {
                warmupCoordinator.toggleSsid(item.ssid());
            }
        });
        recyclerNetworks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerNetworks.setAdapter(networksAdapter);

        btnWarmupRetry.setOnClickListener(v -> {
            cardWarmupFailed.setVisibility(View.GONE);
            if (warmupCoordinator != null) {
                warmupCoordinator.retry();
            }
        });

        btnWarmupContinueIndicative.setOnClickListener(v -> {
            cardWarmupFailed.setVisibility(View.GONE);
            if (warmupCoordinator != null) {
                warmupCoordinator.continueIndicative();
            }
        });

        btnStep2Next.setOnClickListener(v -> moveToStep3());
    }

    private void setupStep3Views() {
        presetList = PresetProfiles.all();
        btnStartSurvey.setOnClickListener(v -> onStartSurveyClicked());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (layoutStep1.getVisibility() == View.VISIBLE) {
            refreshEnvironment();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (warmupCoordinator != null) {
            warmupCoordinator.stop();
        }
    }

    public void refreshEnvironment() {
        if (!isAdded()) {
            return;
        }

        WifiEnvironmentProbe probe = NetFieldApp.wifiEnvironmentProbe();
        boolean hasPerm = probe != null && probe.hasLocationPermission();
        boolean servicesOn = probe != null && probe.isLocationServicesEnabled();
        boolean wifiOn = probe != null && probe.isWifiEnabled();
        EnvironmentAdvisor.ThrottleState throttle = probe != null ? probe.isScanThrottleEnabled() : EnvironmentAdvisor.ThrottleState.UNKNOWN;

        EnvironmentAdvisor.Advice advice = EnvironmentAdvisor.advise(hasPerm, servicesOn, wifiOn, throttle);

        int primaryColor = ContextCompat.getColor(requireContext(), R.color.nf_primary);
        int errorColor = ContextCompat.getColor(requireContext(), R.color.nf_error);
        int warningColor = ContextCompat.getColor(requireContext(), R.color.nf_warning);

        if (hasPerm) {
            textEnvPermission.setText("✓ " + getString(R.string.s06_perm_ok));
            textEnvPermission.setTextColor(primaryColor);
        } else {
            textEnvPermission.setText("✗ " + getString(R.string.s06_perm_missing));
            textEnvPermission.setTextColor(errorColor);
        }

        if (servicesOn) {
            textEnvServices.setText("✓ " + getString(R.string.s06_services_on));
            textEnvServices.setTextColor(primaryColor);
        } else {
            textEnvServices.setText("✗ " + getString(R.string.s06_services_off));
            textEnvServices.setTextColor(errorColor);
        }

        if (wifiOn) {
            textEnvWifi.setText("✓ " + getString(R.string.s06_wifi_on));
            textEnvWifi.setTextColor(primaryColor);
        } else {
            textEnvWifi.setText("✗ " + getString(R.string.s06_wifi_off));
            textEnvWifi.setTextColor(errorColor);
        }

        if (throttle == EnvironmentAdvisor.ThrottleState.ON) {
            textEnvThrottle.setText("⚠ " + getString(R.string.s06_throttle_on));
            textEnvThrottle.setTextColor(warningColor);
            btnDevOptions.setVisibility(View.VISIBLE);
        } else if (throttle == EnvironmentAdvisor.ThrottleState.UNKNOWN) {
            textEnvThrottle.setText("⚠ " + getString(R.string.diag_issue_throttle_unknown));
            textEnvThrottle.setTextColor(warningColor);
            btnDevOptions.setVisibility(View.GONE);
        } else {
            textEnvThrottle.setText("✓ " + getString(R.string.s06_throttle_off));
            textEnvThrottle.setTextColor(primaryColor);
            btnDevOptions.setVisibility(View.GONE);
        }

        List<EnvironmentAdvisor.Issue> issues = advice.issues();
        EnvironmentAdvisor.Issue topFixableIssue = null;
        for (EnvironmentAdvisor.Issue issue : issues) {
            if (issue == EnvironmentAdvisor.Issue.PERMISSION_MISSING
                    || issue == EnvironmentAdvisor.Issue.LOCATION_SERVICES_OFF
                    || issue == EnvironmentAdvisor.Issue.WIFI_OFF) {
                topFixableIssue = issue;
                break;
            }
        }

        if (topFixableIssue != null) {
            cardEnvIssue.setVisibility(View.VISIBLE);
            bindFixableIssue(topFixableIssue);
            btnNext.setEnabled(false);
        } else {
            cardEnvIssue.setVisibility(View.GONE);
            btnNext.setEnabled(advice.isReady());
        }
    }

    private void bindFixableIssue(EnvironmentAdvisor.Issue issue) {
        switch (issue) {
            case PERMISSION_MISSING -> {
                textEnvIssue.setText(R.string.diag_issue_permission);
                PermissionProbe permProbe = NetFieldApp.permissionProbe();
                PermissionPlan plan = null;
                if (getActivity() != null && permProbe != null) {
                    PermissionSnapshot snapshot = permProbe.snapshot(requireActivity());
                    plan = PermissionPlanner.plan(snapshot);
                }

                if (plan != null && plan.location().action() == PermAction.REQUEST) {
                    btnEnvFix.setText(R.string.perm_btn_allow);
                    btnEnvFix.setOnClickListener(v -> permissionLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }));
                } else {
                    btnEnvFix.setText(R.string.diag_fix_permission);
                    btnEnvFix.setOnClickListener(v -> openAppSettings());
                }
            }
            case LOCATION_SERVICES_OFF -> {
                textEnvIssue.setText(R.string.diag_issue_services);
                btnEnvFix.setText(R.string.diag_fix_services);
                btnEnvFix.setOnClickListener(v -> openLocationSettings());
            }
            case WIFI_OFF -> {
                textEnvIssue.setText(R.string.diag_issue_wifi);
                btnEnvFix.setText(R.string.s06_btn_fix);
                btnEnvFix.setOnClickListener(v -> openWifiSettings());
            }
            default -> cardEnvIssue.setVisibility(View.GONE);
        }
    }

    private void openDeveloperOptions() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (ActivityNotFoundException e) {
            View root = getView();
            if (root != null) {
                Snackbar.make(root, R.string.diag_issue_throttle_unknown, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void openAppSettings() {
        if (getActivity() == null) {
            return;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", requireActivity().getPackageName(), null));
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private void openLocationSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } catch (Exception ignored) {
        }
    }

    private void openWifiSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        } catch (Exception ignored) {
        }
    }

    protected void moveToStep2() {
        layoutStep1.setVisibility(View.GONE);
        layoutStep2.setVisibility(View.VISIBLE);
        textStepIndicator.setText(R.string.s06_step2_title);

        if (warmupCoordinator == null) {
            warmupCoordinator = new WarmupCoordinator(
                    NetFieldApp.wifiScanSource(),
                    NetFieldApp.sessionExecutor(),
                    clock,
                    NetFieldApp.wifiEnvironmentProbe()
            );
        }

        warmupCoordinator.setListener(new WarmupCoordinator.Listener() {
            @Override
            public void onProgress(int scansDone, int totalScans) {
                if (!isAdded()) return;
                textWarmupStatus.setText(getString(R.string.s06_warmup_scanning, scansDone, totalScans));
                progressWarmup.setProgress(scansDone);
            }

            @Override
            public void onNetworksUpdated(List<WarmupCoordinator.NetworkGroup> networks, int totalSelectedBssids) {
                if (!isAdded()) return;
                networksAdapter.setItems(networks);
                updateStep2NextState();
            }

            @Override
            public void onWarmupDone(List<Integer> referenceFreqs) {
                if (!isAdded()) return;
                cardWarmupFailed.setVisibility(View.GONE);
                updateStep2NextState();
            }

            @Override
            public void onWarmupFailed() {
                if (!isAdded()) return;
                cardWarmupFailed.setVisibility(View.VISIBLE);
                btnStep2Next.setEnabled(false);
            }

            @Override
            public void onCapExceeded() {
                if (!isAdded()) return;
                View root = getView();
                if (root != null) {
                    Snackbar.make(root, R.string.target_cap_reached, Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        warmupCoordinator.start();
    }

    private void updateStep2NextState() {
        if (warmupCoordinator == null) {
            btnStep2Next.setEnabled(false);
            return;
        }
        boolean isDoneOrIndicative = warmupCoordinator.isCompleted() || warmupCoordinator.isIndicative();
        boolean hasSelectedSsids = !warmupCoordinator.getSelectedSsids().isEmpty();
        btnStep2Next.setEnabled(isDoneOrIndicative && hasSelectedSsids);
    }

    protected void moveToStep3() {
        layoutStep2.setVisibility(View.GONE);
        layoutStep3.setVisibility(View.VISIBLE);
        textStepIndicator.setText(R.string.s06_step3_title);

        // Default name: Survey yyyy-MM-dd HH:mm
        long wallMs = clock.wallMs();
        Instant instant = Instant.ofEpochMilli(wallMs);
        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
        String defaultName = "Survey " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(zdt);
        editSurveyName.setText(defaultName);

        // Indicative warning
        WifiEnvironmentProbe probe = NetFieldApp.wifiEnvironmentProbe();
        boolean throttleOn = (probe != null && probe.isScanThrottleEnabled() == EnvironmentAdvisor.ThrottleState.ON);
        boolean continueIndicativeUsed = (warmupCoordinator != null && warmupCoordinator.isIndicative());
        if (throttleOn || continueIndicativeUsed) {
            textIndicativeWarning.setVisibility(View.VISIBLE);
        } else {
            textIndicativeWarning.setVisibility(View.GONE);
        }

        // Profile picker
        setupProfilePicker();

        // Run latency probe selector
        runLatencyProbeSelector();
    }

    private void setupProfilePicker() {
        Tier currentTier = NetFieldApp.entitlements() != null ? NetFieldApp.entitlements().current() : Tier.FREE;
        List<String> displayNames = new ArrayList<>();
        lastValidProfileIndex = 0;

        for (int i = 0; i < presetList.size(); i++) {
            ProfileSpec spec = presetList.get(i);
            Feature feat = getFeatureForSpec(spec);
            EntitlementDecision decision = EntitlementPolicy.decide(currentTier, feat);
            String name = getDisplayName(spec);
            if (decision.kind() == DecisionKind.LOCKED) {
                displayNames.add("🔒 " + name);
            } else {
                displayNames.add(name);
            }
        }

        isProgrammaticSelection = true;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                displayNames
        );
        spinnerProfile.setAdapter(adapter);

        // Set default selection to OFFICE_STANDARD
        for (int i = 0; i < presetList.size(); i++) {
            if (PresetProfiles.OFFICE_STANDARD.key().equals(presetList.get(i).key())) {
                spinnerProfile.setSelection(i);
                selectedProfileId = PresetProfiles.idFor(PresetProfiles.OFFICE_STANDARD.key());
                lastValidProfileIndex = i;
                break;
            }
        }
        isProgrammaticSelection = false;

        spinnerProfile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isProgrammaticSelection) {
                    return;
                }
                if (position == lastValidProfileIndex) {
                    return;
                }
                ProfileSpec spec = presetList.get(position);
                Feature feat = getFeatureForSpec(spec);
                EntitlementDecision decision = EntitlementPolicy.decide(currentTier, feat);
                if (decision.kind() == DecisionKind.LOCKED) {
                    NavController nav = NavHostFragment.findNavController(NewSurveyFragment.this);
                    if (nav.getCurrentDestination() != null && nav.getCurrentDestination().getId() == R.id.nav_s06) {
                        Bundle args = new Bundle();
                        args.putString(NavArgs.ARG_FEATURE, feat.name());
                        args.putString(NavArgs.FEATURE, feat.name());
                        nav.navigate(R.id.action_s06_to_s18, args);
                    }
                    isProgrammaticSelection = true;
                    spinnerProfile.setSelection(lastValidProfileIndex);
                    isProgrammaticSelection = false;
                } else {
                    lastValidProfileIndex = position;
                    selectedProfileId = PresetProfiles.idFor(spec.key());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private Feature getFeatureForSpec(ProfileSpec spec) {
        return switch (spec.key()) {
            case "OFFICE_STANDARD" -> Feature.PRESET_OFFICE;
            case "WAREHOUSE_ROBOTICS" -> Feature.PRESET_WAREHOUSE;
            case "CCTV_SURVEILLANCE" -> Feature.PRESET_CCTV;
            case "SMART_HOME_IOT" -> Feature.PRESET_SMART_HOME;
            default -> Feature.PRESET_OFFICE;
        };
    }

    private String getDisplayName(ProfileSpec spec) {
        return switch (spec.key()) {
            case "OFFICE_STANDARD" -> "Office standard";
            case "WAREHOUSE_ROBOTICS" -> "Warehouse robotics";
            case "CCTV_SURVEILLANCE" -> "CCTV surveillance";
            case "SMART_HOME_IOT" -> "Smart home IoT";
            default -> spec.key();
        };
    }

    private void runLatencyProbeSelector() {
        WifiNetworkTracker tracker = NetFieldApp.wifiNetworkTracker();
        Network network = tracker != null ? tracker.getCurrentNetwork() : null;
        String gateway = tracker != null ? tracker.getGateway() : null;
        String dns = tracker != null ? tracker.getDns() : null;

        List<ProbeCandidate> candidates = ProbePlanner.plan(gateway, dns);
        if (candidates.isEmpty()) {
            selectedProbeMethod = "NONE";
            textLatencyMethod.setText(R.string.s06_latency_not_available);
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            ProbeCandidate winner = null;
            try {
                ProbeMethodSelector.SelectionResult selection = ProbeMethodSelector.select(
                        candidates,
                        c -> {
                            if (c.method() == ProbeMethod.ICMP) {
                                return new DgramIcmpProbe(network, c.target(), clock, () -> false);
                            } else {
                                return new TcpConnectProbe(network, c.target(), c.port(), clock, () -> false);
                            }
                        }
                );
                if (!selection.unreachable() && selection.selected() != null) {
                    winner = selection.selected();
                }
            } catch (Exception ignored) {
            }

            ProbeCandidate finalWinner = winner;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (finalWinner != null) {
                        if (finalWinner.method() == ProbeMethod.ICMP) {
                            selectedProbeMethod = "ICMP";
                            textLatencyMethod.setText("Latency: Gateway (ICMP)");
                        } else {
                            selectedProbeMethod = "TCP";
                            if (finalWinner.target().equals(gateway)) {
                                textLatencyMethod.setText("Latency: Gateway (TCP)");
                            } else {
                                textLatencyMethod.setText("Latency: DNS (TCP)");
                            }
                        }
                    } else {
                        selectedProbeMethod = "NONE";
                        textLatencyMethod.setText(R.string.s06_latency_not_available);
                    }
                });
            }
        });
    }

    private void onStartSurveyClicked() {
        String nameText = editSurveyName.getText() != null ? editSurveyName.getText().toString() : "";
        NameRules.Status nameStatus = NameRules.validate(nameText);
        if (nameStatus == NameRules.Status.EMPTY) {
            editSurveyName.setError(getString(R.string.s06_error_empty_name));
            return;
        } else if (nameStatus == NameRules.Status.TOO_LONG) {
            editSurveyName.setError(getString(R.string.s06_error_long_name));
            return;
        }
        editSurveyName.setError(null);

        // Quota check
        QuotaDecision decision = NetFieldApp.quotaGuard().check(Resource.SESSION);
        if (!decision.allowed()) {
            Bundle args = new Bundle();
            args.putString(NavArgs.QUOTA, "SESSION");
            NavHostFragment.findNavController(this).navigate(R.id.action_s06_to_s18, args);
            return;
        }

        List<String> selectedSsids = warmupCoordinator != null ? warmupCoordinator.getSelectedSsids() : Collections.emptyList();
        if (selectedSsids.isEmpty()) {
            View root = getView();
            if (root != null) {
                Snackbar.make(root, R.string.s06_error_no_ssid, Snackbar.LENGTH_SHORT).show();
            }
            return;
        }

        WifiEnvironmentProbe probe = NetFieldApp.wifiEnvironmentProbe();
        boolean throttleOn = (probe != null && probe.isScanThrottleEnabled() == EnvironmentAdvisor.ThrottleState.ON);
        boolean continueIndicativeUsed = (warmupCoordinator != null && warmupCoordinator.isIndicative());
        String evidenceGrade = (throttleOn || continueIndicativeUsed) ? "INDICATIVE" : "VERDICT";

        String tierName = (NetFieldApp.wifiScanSource() != null && NetFieldApp.wifiScanSource().getTier() != null)
                ? NetFieldApp.wifiScanSource().getTier().name()
                : "CALLBACK";
        List<Integer> refFreqs = warmupCoordinator != null ? warmupCoordinator.getReferenceFreqs() : Collections.emptyList();

        SessionRepository repo = sessionRepo != null ? sessionRepo : SessionRepository.create(requireContext(), clock);
        SurveySessionRow session = repo.createFull(
                floorId,
                NameRules.normalize(nameText),
                null,
                throttleOn,
                evidenceGrade,
                selectedProfileId,
                selectedProbeMethod,
                refFreqs,
                tierName,
                selectedSsids
        );

        if (NetFieldApp.sessionExecutor() != null) {
            NetFieldApp.sessionExecutor().setCurrentSessionId(session.id);
        }

        Bundle args = new Bundle();
        args.putString(NavArgs.SESSION_ID, session.id);
        NavHostFragment.findNavController(this).navigate(R.id.action_s06_to_s07, args);
    }

    public WarmupCoordinator getWarmupCoordinator() {
        return warmupCoordinator;
    }

    @VisibleForTesting
    public void setWarmupCoordinatorForTest(WarmupCoordinator coordinator) {
        this.warmupCoordinator = coordinator;
    }

    @VisibleForTesting
    public void setClockForTest(Clock clock) {
        this.clock = clock;
    }

    @VisibleForTesting
    public void setSessionRepositoryForTest(SessionRepository repo) {
        this.sessionRepo = repo;
    }
}
