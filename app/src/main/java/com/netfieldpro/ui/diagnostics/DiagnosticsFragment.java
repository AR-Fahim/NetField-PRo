package com.netfieldpro.ui.diagnostics;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.netfieldpro.BuildConfig;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.latency.LatencyStats;
import com.netfieldpro.core.latency.ProbeCandidate;
import com.netfieldpro.core.model.enums.ProbeMethod;
import com.netfieldpro.core.trace.TraceHeader;
import com.netfieldpro.core.trace.TraceWriter;
import com.netfieldpro.core.util.FixedDecimal;
import com.netfieldpro.core.wifi.EnvironmentAdvisor;
import com.netfieldpro.core.wifi.RecordingWifiScanSource;
import com.netfieldpro.core.wifi.ScanListModel;
import com.netfieldpro.core.wifi.Tier;
import com.netfieldpro.core.wifi.WifiScanSource;
import com.netfieldpro.time.AndroidClock;
import com.netfieldpro.wifi.WifiEnvironmentProbe;

public class DiagnosticsFragment extends Fragment {

    private DiagnosticsViewModel viewModel;
    private DiagnosticsNetworkAdapter adapter;

    private MaterialCardView cardDiagEnv;
    private TextView textPermissionValue;
    private TextView textServicesValue;
    private TextView textWifiValue;
    private TextView textThrottleValue;
    private TextView textMethodValue;

    private MaterialButton btnDiagScan;
    private TextView textDiagLastScan;
    private TextView textDiagEmpty;
    private RecyclerView listDiagNetworks;

    private MaterialCardView cardDiagIssue;
    private TextView textDiagIssue;
    private MaterialButton btnDiagIssueFix;

    private MaterialCardView cardDiagLatency;
    private TextView textDiagLatencyTarget;
    private TextView textDiagLatencyValues;
    private TextView textDiagLatencySummary;
    private TextView textDiagLatencyState;
    private MaterialButton btnDiagLatency;

    private WifiScanSource baseScanSource;
    private RecordingWifiScanSource activeRecordingSource;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diagnostics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DiagnosticsViewModel.class);

        cardDiagEnv = view.findViewById(R.id.card_diag_env);
        textPermissionValue = view.findViewById(R.id.text_diag_env_permission_value);
        textServicesValue = view.findViewById(R.id.text_diag_env_services_value);
        textWifiValue = view.findViewById(R.id.text_diag_env_wifi_value);
        textThrottleValue = view.findViewById(R.id.text_diag_env_throttle_value);
        textMethodValue = view.findViewById(R.id.text_diag_env_method_value);

        btnDiagScan = view.findViewById(R.id.btn_diag_scan);
        textDiagLastScan = view.findViewById(R.id.text_diag_last_scan);
        textDiagEmpty = view.findViewById(R.id.text_diag_empty);
        listDiagNetworks = view.findViewById(R.id.list_diag_networks);

        cardDiagIssue = view.findViewById(R.id.card_diag_issue);
        textDiagIssue = view.findViewById(R.id.text_diag_issue);
        btnDiagIssueFix = view.findViewById(R.id.btn_diag_issue_fix);

        cardDiagLatency = view.findViewById(R.id.card_diag_latency);
        textDiagLatencyTarget = view.findViewById(R.id.text_diag_latency_target);
        textDiagLatencyValues = view.findViewById(R.id.text_diag_latency_values);
        textDiagLatencySummary = view.findViewById(R.id.text_diag_latency_summary);
        textDiagLatencyState = view.findViewById(R.id.text_diag_latency_state);
        btnDiagLatency = view.findViewById(R.id.btn_diag_latency);

        adapter = new DiagnosticsNetworkAdapter();
        listDiagNetworks.setLayoutManager(new LinearLayoutManager(requireContext()));
        listDiagNetworks.setAdapter(adapter);

        btnDiagScan.setOnClickListener(v -> viewModel.scanNow());
        btnDiagLatency.setOnClickListener(v -> viewModel.startOrCancelLatencyProbe());

        observeViewModel();
        setupDebugRecording(view);
    }

    private void observeViewModel() {
        viewModel.getAdvice().observe(getViewLifecycleOwner(), this::bindAdvice);
        viewModel.getTier().observe(getViewLifecycleOwner(), this::bindTier);
        viewModel.getNetworks().observe(getViewLifecycleOwner(), this::bindNetworks);
        viewModel.getIsScanning().observe(getViewLifecycleOwner(), this::bindScanningState);
        viewModel.getLatencyState().observe(getViewLifecycleOwner(), this::bindLatencyState);
        viewModel.getThrottledEvent().observe(getViewLifecycleOwner(), throttled -> {
            if (Boolean.TRUE.equals(throttled)) {
                View root = getView();
                if (root != null) {
                    Snackbar.make(root, R.string.diag_throttled, Snackbar.LENGTH_LONG).show();
                }
                viewModel.clearThrottledEvent();
            }
        });
    }

    private void bindAdvice(EnvironmentAdvisor.Advice advice) {
        if (advice == null) {
            return;
        }

        WifiEnvironmentProbe probe = NetFieldApp.wifiEnvironmentProbe();
        boolean hasPerm = probe != null && probe.hasLocationPermission();
        boolean servicesOn = probe != null && probe.isLocationServicesEnabled();
        boolean wifiOn = probe != null && probe.isWifiEnabled();
        EnvironmentAdvisor.ThrottleState throttle = probe != null ? probe.isScanThrottleEnabled() : EnvironmentAdvisor.ThrottleState.UNKNOWN;

        textPermissionValue.setText(hasPerm ? R.string.diag_allowed : R.string.diag_not_allowed);
        textServicesValue.setText(servicesOn ? R.string.diag_on : R.string.diag_off);
        textWifiValue.setText(wifiOn ? R.string.diag_on : R.string.diag_off);

        int warningColor = ContextCompat.getColor(requireContext(), R.color.nf_warning);
        if (throttle == EnvironmentAdvisor.ThrottleState.ON) {
            textThrottleValue.setText("⚠ " + getString(R.string.diag_on));
            textThrottleValue.setTextColor(warningColor);
        } else if (throttle == EnvironmentAdvisor.ThrottleState.UNKNOWN) {
            textThrottleValue.setText("⚠ " + getString(R.string.diag_unknown));
            textThrottleValue.setTextColor(warningColor);
        } else {
            textThrottleValue.setText(R.string.diag_off);
            textThrottleValue.setTextColor(textPermissionValue.getTextColors());
        }

        List<EnvironmentAdvisor.Issue> issues = advice.issues();
        if (issues.isEmpty()) {
            cardDiagIssue.setVisibility(View.GONE);
            btnDiagScan.setEnabled(true);
        } else {
            cardDiagIssue.setVisibility(View.VISIBLE);
            EnvironmentAdvisor.Issue topIssue = issues.get(0);
            bindIssue(topIssue, advice.isReady());
        }
    }

    private void bindIssue(EnvironmentAdvisor.Issue issue, boolean isReady) {
        switch (issue) {
            case PERMISSION_MISSING -> {
                textDiagIssue.setText(R.string.diag_issue_permission);
                btnDiagIssueFix.setText(R.string.diag_fix_permission);
                btnDiagIssueFix.setVisibility(View.VISIBLE);
                btnDiagIssueFix.setOnClickListener(v -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
                    startActivity(intent);
                });
                btnDiagScan.setEnabled(false);
            }
            case LOCATION_SERVICES_OFF -> {
                textDiagIssue.setText(R.string.diag_issue_services);
                btnDiagIssueFix.setText(R.string.diag_fix_services);
                btnDiagIssueFix.setVisibility(View.VISIBLE);
                btnDiagIssueFix.setOnClickListener(v -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                });
                btnDiagScan.setEnabled(false);
            }
            case WIFI_OFF -> {
                textDiagIssue.setText(R.string.diag_issue_wifi);
                btnDiagIssueFix.setVisibility(View.GONE);
                btnDiagScan.setEnabled(false);
            }
            case THROTTLE_ON -> {
                textDiagIssue.setText(R.string.diag_issue_throttle_on);
                btnDiagIssueFix.setVisibility(View.GONE);
                btnDiagScan.setEnabled(isReady);
            }
            case THROTTLE_UNKNOWN -> {
                textDiagIssue.setText(R.string.diag_issue_throttle_unknown);
                btnDiagIssueFix.setVisibility(View.GONE);
                btnDiagScan.setEnabled(isReady);
            }
        }
    }

    private void bindTier(Tier tier) {
        if (tier == null) {
            return;
        }
        switch (tier) {
            case CALLBACK -> textMethodValue.setText(R.string.diag_method_callback);
            case RECEIVER -> textMethodValue.setText(R.string.diag_method_receiver);
            case POLL -> textMethodValue.setText(R.string.diag_method_poll);
        }
    }

    private void bindNetworks(List<ScanListModel.Row> rows) {
        if (rows == null || rows.isEmpty()) {
            textDiagEmpty.setVisibility(View.VISIBLE);
            listDiagNetworks.setVisibility(View.GONE);
            textDiagLastScan.setVisibility(View.GONE);
        } else {
            textDiagEmpty.setVisibility(View.GONE);
            listDiagNetworks.setVisibility(View.VISIBLE);
            adapter.setItems(rows);
            textDiagLastScan.setVisibility(View.VISIBLE);
            textDiagLastScan.setText(getString(R.string.diag_last_scan, getString(R.string.diag_ago_seconds, 0), rows.size()));
        }
    }

    private void bindScanningState(Boolean isScanning) {
        if (Boolean.TRUE.equals(isScanning)) {
            btnDiagScan.setText(R.string.diag_scanning);
            btnDiagScan.setEnabled(false);
        } else {
            btnDiagScan.setText(R.string.diag_scan_now);
            EnvironmentAdvisor.Advice advice = viewModel.getAdvice().getValue();
            boolean ready = advice == null || advice.isReady();
            btnDiagScan.setEnabled(ready);
        }
    }

    private void bindLatencyState(DiagnosticsViewModel.LatencyUiModel model) {
        if (model == null) {
            return;
        }
        switch (model.state) {
            case NO_WIFI -> {
                textDiagLatencyTarget.setVisibility(View.GONE);
                textDiagLatencyValues.setVisibility(View.GONE);
                textDiagLatencySummary.setVisibility(View.GONE);
                textDiagLatencyState.setVisibility(View.VISIBLE);
                textDiagLatencyState.setText(R.string.diag_latency_no_wifi);
                btnDiagLatency.setText(R.string.diag_latency_probe);
                btnDiagLatency.setEnabled(false);
            }
            case IDLE_READY -> {
                textDiagLatencyTarget.setVisibility(View.GONE);
                textDiagLatencyValues.setVisibility(View.GONE);
                textDiagLatencySummary.setVisibility(View.GONE);
                textDiagLatencyState.setVisibility(View.GONE);
                btnDiagLatency.setText(R.string.diag_latency_probe);
                btnDiagLatency.setEnabled(true);
            }
            case RUNNING -> {
                textDiagLatencyTarget.setVisibility(View.GONE);
                textDiagLatencyValues.setVisibility(View.GONE);
                textDiagLatencySummary.setVisibility(View.GONE);
                textDiagLatencyState.setVisibility(View.VISIBLE);
                textDiagLatencyState.setText(R.string.diag_latency_running);
                btnDiagLatency.setText(R.string.diag_latency_cancel);
                btnDiagLatency.setEnabled(true);
            }
            case RESULT -> {
                textDiagLatencyState.setVisibility(View.GONE);
                btnDiagLatency.setText(R.string.diag_latency_probe);
                btnDiagLatency.setEnabled(true);

                if (model.candidate != null) {
                    String methodStr;
                    if (model.candidate.method() == ProbeMethod.ICMP) {
                        methodStr = getString(R.string.diag_latency_method_icmp);
                    } else {
                        methodStr = getString(R.string.diag_latency_method_tcp, model.candidate.port());
                    }
                    textDiagLatencyTarget.setText(getString(R.string.diag_latency_target, model.candidate.target(), methodStr));
                    textDiagLatencyTarget.setVisibility(View.VISIBLE);
                } else {
                    textDiagLatencyTarget.setVisibility(View.GONE);
                }

                if (model.sequenceResult != null && model.sequenceResult.rttUsOrLost() != null) {
                    StringBuilder sb = new StringBuilder();
                    for (long rtt : model.sequenceResult.rttUsOrLost()) {
                        if (sb.length() > 0) {
                            sb.append(' ');
                        }
                        if (rtt == LatencyStats.LOST || rtt < 0) {
                            sb.append(getString(R.string.diag_latency_lost_mark));
                        } else {
                            sb.append(Math.round(rtt / 1000.0));
                        }
                    }
                    textDiagLatencyValues.setText(sb.toString());
                    textDiagLatencyValues.setVisibility(View.VISIBLE);

                    LatencyStats.Summary s = model.sequenceResult.summary();
                    if (s != null) {
                        String avgStr = s.avgMs() != null ? FixedDecimal.format(s.avgMs(), 1) : getString(R.string.diag_latency_none);
                        String jitterStr = s.jitterMs() != null ? FixedDecimal.format(s.jitterMs(), 1) : getString(R.string.diag_latency_none);
                        textDiagLatencySummary.setText(getString(R.string.diag_latency_summary, avgStr, jitterStr, s.lost(), s.sent()));
                        textDiagLatencySummary.setVisibility(View.VISIBLE);
                    } else {
                        textDiagLatencySummary.setVisibility(View.GONE);
                    }
                } else {
                    textDiagLatencyValues.setVisibility(View.GONE);
                    textDiagLatencySummary.setVisibility(View.GONE);
                }
            }
            case BLOCKED -> {
                textDiagLatencyTarget.setVisibility(View.GONE);
                textDiagLatencyValues.setVisibility(View.GONE);
                textDiagLatencySummary.setVisibility(View.GONE);
                textDiagLatencyState.setVisibility(View.VISIBLE);
                textDiagLatencyState.setText(R.string.diag_latency_blocked);
                btnDiagLatency.setText(R.string.diag_latency_probe);
                btnDiagLatency.setEnabled(true);
            }
            case NETWORK_LOST -> {
                textDiagLatencyTarget.setVisibility(View.GONE);
                textDiagLatencyValues.setVisibility(View.GONE);
                textDiagLatencySummary.setVisibility(View.GONE);
                textDiagLatencyState.setVisibility(View.VISIBLE);
                textDiagLatencyState.setText(R.string.diag_latency_net_lost);
                btnDiagLatency.setText(R.string.diag_latency_probe);
                btnDiagLatency.setEnabled(false);
            }
        }
    }

    private void setupDebugRecording(View root) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        FrameLayout container = root.findViewById(R.id.layout_diag_dev_container);
        if (container == null) {
            return;
        }

        int layoutId = getResources().getIdentifier("layout_debug_" + "diag_record", "layout", requireContext().getPackageName());
        if (layoutId == 0) {
            return;
        }

        View debugView = getLayoutInflater().inflate(layoutId, container, false);
        container.addView(debugView);

        int switchId = getResources().getIdentifier("switch_diag_" + "record", "id", requireContext().getPackageName());
        int pathId = getResources().getIdentifier("text_diag_record_" + "path", "id", requireContext().getPackageName());

        if (switchId != 0) {
            android.widget.CompoundButton switchRecord = debugView.findViewById(switchId);
            TextView textPath = (pathId != 0) ? debugView.findViewById(pathId) : null;
            if (switchRecord != null) {
                switchRecord.setOnCheckedChangeListener((btn, isChecked) -> {
                    handleDebugRecordToggle(isChecked, textPath);
                });
            }
        }

        int forcePollSwitchId = getResources().getIdentifier("switch_diag_" + "force_poll", "id", requireContext().getPackageName());
        if (forcePollSwitchId != 0) {
            android.widget.CompoundButton switchForcePoll = debugView.findViewById(forcePollSwitchId);
            if (switchForcePoll != null) {
                android.content.SharedPreferences debugPrefs = requireContext().getSharedPreferences("nf_debug", android.content.Context.MODE_PRIVATE);
                switchForcePoll.setChecked(debugPrefs.getBoolean("scan_force_poll", false));
                switchForcePoll.setOnCheckedChangeListener((btn, isChecked) -> {
                    debugPrefs.edit().putBoolean("scan_force_poll", isChecked).apply();
                    viewModel.restartScanSource();
                });
            }
        }
    }

    private void handleDebugRecordToggle(boolean isChecked, TextView textPath) {
        if (isChecked) {
            try {
                File dir = requireContext().getExternalFilesDir("traces");
                if (dir != null) {
                    dir.mkdirs();
                }
                long wallMs = new AndroidClock().wallMs();
                long monoNs = new AndroidClock().monoNs();
                ZonedDateTime zdt = Instant.ofEpochMilli(wallMs).atZone(ZoneOffset.UTC);
                String ts = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(zdt);
                File file = new File(dir, "wifi-" + ts + ".nftrace");

                TraceHeader header = new TraceHeader(1, Build.MODEL, Build.VERSION.SDK_INT, wallMs, monoNs);
                TraceWriter writer = new TraceWriter(new FileOutputStream(file), header);

                baseScanSource = viewModel.getScanSource();
                activeRecordingSource = new RecordingWifiScanSource(baseScanSource, writer);
                viewModel.setScanSource(activeRecordingSource);

                if (textPath != null) {
                    textPath.setText(file.getAbsolutePath());
                    textPath.setVisibility(View.VISIBLE);
                }
            } catch (Exception ignored) {
            }
        } else {
            if (activeRecordingSource != null) {
                try {
                    activeRecordingSource.close();
                } catch (Exception ignored) {
                }
                activeRecordingSource = null;
            }
            if (baseScanSource != null) {
                viewModel.setScanSource(baseScanSource);
            }
            if (textPath != null) {
                textPath.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        viewModel.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        viewModel.onStop();
        if (activeRecordingSource != null) {
            try {
                activeRecordingSource.close();
            } catch (Exception ignored) {
            }
            activeRecordingSource = null;
        }
    }
}
