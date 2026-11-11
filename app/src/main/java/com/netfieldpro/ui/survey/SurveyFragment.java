package com.netfieldpro.ui.survey;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.power.BatteryAdvisor.Level;
import com.netfieldpro.core.scale.Affine3x3;
import com.netfieldpro.core.session.SessionEvent;
import com.netfieldpro.core.session.Sm1State;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.MeasurementRow;
import com.netfieldpro.data.db.entity.SurveyPointRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.db.entity.TargetNetworkRow;
import com.netfieldpro.data.repository.SessionRepository;
import com.netfieldpro.power.BatteryAlertMonitor;
import com.netfieldpro.session.SessionExecutor;
import com.netfieldpro.time.AndroidClock;
import com.netfieldpro.ui.plan.PlanView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SurveyFragment extends Fragment {

    private static Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    @androidx.annotation.VisibleForTesting
    public static void setBackgroundExecutorForTest(Executor executor) {
        backgroundExecutor = executor != null ? executor : Executors.newSingleThreadExecutor();
    }

    private String sessionId;
    private double metersPerPixel = 0.05;
    private String floorId;
    private boolean readOnly = false;

    private PlanView planView;
    private TextView textScreenTitle;
    private TextView textPinCount;
    private MaterialButton btnViewHeatmap;
    private MaterialButton btnOverflow;
    private TextView bannerReadonly;
    private MaterialCardView cardEmptyHint;
    private TextView textEmptyHint;
    private MaterialCardView sheetCapture;
    private TextView textCaptureStep;
    private MaterialButton btnCaptureCancel;
    private View bannerBattery;
    private TextView textBatteryTitle;
    private TextView textBatteryHint;
    private MaterialButton btnBatteryEnd;
    private TextView bannerAirplane;
    private MaterialButton btnWalkS08;
    private MaterialButton btnWalkS09;

    private CaptureCoordinator captureCoordinator;
    private final com.netfieldpro.core.time.Clock clock = new AndroidClock();
    private final List<SurveyPointRow> surveyPoints = new ArrayList<>();
    private final Map<String, Integer> pointBestRssiMap = new HashMap<>();

    private final Paint pinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            sessionId = getArguments().getString("sessionId");
        }

        pinPaint.setStyle(Paint.Style.FILL);
        pinStrokePaint.setStyle(Paint.Style.STROKE);
        pinStrokePaint.setColor(Color.WHITE);
        pinStrokePaint.setStrokeWidth(3.0f);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(24.0f);
        textPaint.setFakeBoldText(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_survey, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        planView = view.findViewById(R.id.plan_view);
        textScreenTitle = view.findViewById(R.id.text_screen_title);
        textPinCount = view.findViewById(R.id.text_pin_count);
        btnViewHeatmap = view.findViewById(R.id.btn_view_heatmap);
        btnOverflow = view.findViewById(R.id.btn_overflow);
        bannerReadonly = view.findViewById(R.id.banner_readonly);
        cardEmptyHint = view.findViewById(R.id.card_empty_hint);
        textEmptyHint = view.findViewById(R.id.text_empty_hint);
        sheetCapture = view.findViewById(R.id.sheet_capture);
        textCaptureStep = view.findViewById(R.id.text_capture_step);
        btnCaptureCancel = view.findViewById(R.id.btn_capture_cancel);

        bannerBattery = view.findViewById(R.id.banner_battery);
        textBatteryTitle = view.findViewById(R.id.text_battery_title);
        textBatteryHint = view.findViewById(R.id.text_battery_hint);
        btnBatteryEnd = view.findViewById(R.id.btn_battery_end);
        if (btnBatteryEnd != null) {
            btnBatteryEnd.setOnClickListener(v -> showEndSurveyConfirm());
        }
        bannerAirplane = view.findViewById(R.id.banner_airplane);

        btnWalkS08 = view.findViewById(R.id.btn_walk_s08);
        btnWalkS09 = view.findViewById(R.id.btn_walk_s09);

        if (btnWalkS08 != null) {
            btnWalkS08.setOnClickListener(v -> {
                Bundle b = new Bundle();
                b.putString("sessionId", sessionId);
                NavHostFragment.findNavController(this).navigate(R.id.action_s07_to_s08, b);
            });
        }
        if (btnWalkS09 != null) {
            btnWalkS09.setOnClickListener(v -> navigateToHeatmap());
        }

        btnViewHeatmap.setOnClickListener(v -> navigateToHeatmap());
        btnCaptureCancel.setOnClickListener(v -> {
            if (captureCoordinator != null) {
                captureCoordinator.cancelCapture();
            }
        });

        if (btnOverflow != null) {
            btnOverflow.setOnClickListener(this::showOverflowMenu);
        }

        View actionEndSurvey = view.findViewById(R.id.action_end_survey);
        if (actionEndSurvey != null) {
            actionEndSurvey.setOnClickListener(v -> showEndSurveyConfirm());
        }

        setupPlanView();
        loadSessionData();
        observeBatteryAlerts();
    }

    private void observeBatteryAlerts() {
        BatteryAlertMonitor monitor = NetFieldApp.batteryAlerts();
        if (monitor == null) {
            return;
        }

        monitor.level().observe(getViewLifecycleOwner(), level -> updateBatteryBanner(level, monitor.percent().getValue()));
        monitor.percent().observe(getViewLifecycleOwner(), pct -> updateBatteryBanner(monitor.level().getValue(), pct));
        monitor.airplane().observe(getViewLifecycleOwner(), isAirplane -> {
            if (bannerAirplane != null) {
                boolean show = Boolean.TRUE.equals(isAirplane);
                bannerAirplane.setVisibility(show ? View.VISIBLE : View.GONE);
                if (show) {
                    bannerAirplane.announceForAccessibility(getString(R.string.banner_airplane));
                }
            }
        });
    }

    private void updateBatteryBanner(Level level, Integer pct) {
        if (bannerBattery == null || textBatteryTitle == null || textBatteryHint == null) {
            return;
        }
        if (level == null || level == Level.NONE) {
            bannerBattery.setVisibility(View.GONE);
            return;
        }

        int percentVal = (pct != null) ? pct : 0;
        String title;
        String hint;
        if (level == Level.CRITICAL) {
            title = getString(R.string.banner_batt_crit, percentVal);
            hint = getString(R.string.banner_batt_hint_crit);
            textBatteryTitle.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.nf_error));
        } else {
            title = getString(R.string.banner_batt_warn, percentVal);
            hint = getString(R.string.banner_batt_hint_warn);
            textBatteryTitle.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.nf_warning));
        }

        textBatteryTitle.setText(title);
        textBatteryHint.setText(hint);
        bannerBattery.setVisibility(View.VISIBLE);
        bannerBattery.announceForAccessibility(title + ". " + hint);
    }

    private void setupPlanView() {
        planView.setOverlayDrawer((canvas, transform) -> drawPinsOverlay(canvas, transform));

        planView.setPlanTapListener((imgX, imgY) -> {
            if (readOnly) {
                return;
            }
            if (captureCoordinator != null && captureCoordinator.isCapturing()) {
                return;
            }

            // Check if tapped near an existing point (within ~30 image pixels)
            for (SurveyPointRow pt : surveyPoints) {
                double px = pt.x / metersPerPixel;
                double py = pt.y / metersPerPixel;
                double dist = Math.hypot(imgX - px, imgY - py);
                if (dist <= 30.0) {
                    openPointDetail(pt.id);
                    return;
                }
            }

            // Otherwise, start capture at tapped position
            if (captureCoordinator != null) {
                planView.setDraggablePin(imgX, imgY);
                if (cardEmptyHint != null) {
                    cardEmptyHint.setVisibility(View.GONE);
                }
                if (sheetCapture != null) {
                    sheetCapture.setVisibility(View.VISIBLE);
                }
                captureCoordinator.startCapture(imgX, imgY);
            }
        });

        planView.setPinDragListener((imgX, imgY) -> {
            if (captureCoordinator != null && captureCoordinator.isCapturing()) {
                captureCoordinator.dragPin(imgX, imgY);
            }
        });
    }

    private void drawPinsOverlay(Canvas canvas, Affine3x3 transform) {
        float radiusPx = 14.0f * getResources().getDisplayMetrics().density;
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < surveyPoints.size(); i++) {
            SurveyPointRow pt = surveyPoints.get(i);
            double imgX = pt.x / metersPerPixel;
            double imgY = pt.y / metersPerPixel;

            float vx = (float) transform.transformX(imgX, imgY);
            float vy = (float) transform.transformY(imgX, imgY);

            Integer bestRssi = pointBestRssiMap.get(pt.id);
            int color = getPinColor(bestRssi != null ? bestRssi : -70);
            pinPaint.setColor(color);

            canvas.drawCircle(vx, vy, radiusPx, pinPaint);
            canvas.drawCircle(vx, vy, radiusPx, pinStrokePaint);

            String label = String.valueOf(i + 1);
            float textOffset = (textPaint.descent() + textPaint.ascent()) / 2.0f;
            canvas.drawText(label, vx, vy - textOffset, textPaint);
        }
    }

    private int getPinColor(int rssiDbm) {
        if (rssiDbm >= -65) {
            return 0xFF2E7D32; // Green
        } else if (rssiDbm >= -75) {
            return 0xFFF57F17; // Amber
        } else {
            return 0xFFC62828; // Red
        }
    }

    private void loadSessionData() {
        if (sessionId == null && getContext() != null) {
            // Find most recent draft session if not passed
            backgroundExecutor.execute(() -> {
                List<SurveySessionRow> drafts = NetFieldDatabase.get(requireContext()).netFieldDao().getDraftSessions();
                if (!drafts.isEmpty()) {
                    sessionId = drafts.get(0).id;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(this::loadSessionData);
                    }
                }
            });
            return;
        }

        if (sessionId == null || getContext() == null) {
            return;
        }

        backgroundExecutor.execute(() -> {
            NetFieldDatabase db = NetFieldDatabase.get(requireContext());
            SurveySessionRow session = db.netFieldDao().getSurveySession(sessionId);
            if (session == null) {
                return;
            }

            floorId = session.floorId;
            FloorRow floor = db.netFieldDao().getFloor(floorId);
            if (floor != null && floor.metersPerPixel > 0) {
                metersPerPixel = floor.metersPerPixel;
            }

            List<TargetNetworkRow> targetRows = db.netFieldDao().getTargetNetworksForSession(sessionId);
            Set<String> targetSsids = new HashSet<>();
            for (TargetNetworkRow tn : targetRows) {
                if (tn.ssid != null && !tn.ssid.isEmpty()) {
                    targetSsids.add(tn.ssid);
                }
            }

            List<Integer> refFreqs = parseReferenceFreqs(session.referenceFreqsMhz);

            SessionRepository sessionRepo = SessionRepository.create(requireContext());
            Sm1State storeState = sessionRepo.getRuntimeStore().getState(sessionId);
            boolean isReadOnly = "FINALIZED".equalsIgnoreCase(session.state) || storeState == Sm1State.ENDED;

            File planFile = NetFieldApp.planFileStore().findPlanFile(floorId);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    readOnly = isReadOnly;
                    bannerReadonly.setVisibility(readOnly ? View.VISIBLE : View.GONE);
                    textScreenTitle.setText(session.name);

                    if (planFile != null && planFile.exists()) {
                        planView.loadPlanFile(planFile);
                    }

                    initCaptureCoordinator(session, refFreqs, targetSsids);
                    loadPoints();
                });
            }
        });
    }

    private void initCaptureCoordinator(SurveySessionRow session, List<Integer> refFreqs, Set<String> targetSsids) {
        if (captureCoordinator == null) {
            captureCoordinator = new CaptureCoordinator(
                    sessionId,
                    metersPerPixel,
                    refFreqs,
                    targetSsids,
                    session.probeMethod,
                    NetFieldApp.sessionExecutor(),
                    NetFieldApp.wifiScanSource(),
                    NetFieldApp.sensorSource(),
                    NetFieldApp.latencyRunner(),
                    NetFieldApp.wifiEnvironmentProbe(),
                    new com.netfieldpro.data.db.BatchedWriter(NetFieldDatabase.get(requireContext())),
                    NetFieldDatabase.get(requireContext()),
                    clock,
                    new CaptureCoordinator.HandlerTimeoutScheduler()
            );
        }

        captureCoordinator.setListener(new CaptureCoordinator.Listener() {
            @Override
            public void onPinPosition(double x, double y) {
                planView.setDraggablePin(x, y);
            }

            @Override
            public void onCaptureStep(String stepDescription, int currentScan, int totalScans) {
                if (textCaptureStep != null) {
                    textCaptureStep.setText(stepDescription);
                }
            }

            @Override
            public void onCaptureCommitted(String pointId, String quality) {
                planView.clearDraggablePin();
                if (sheetCapture != null) {
                    sheetCapture.setVisibility(View.GONE);
                }
                View root = getView();
                if (root != null) {
                    root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }
                loadPoints();
            }

            @Override
            public void onCaptureDiscarded(String reason) {
                planView.clearDraggablePin();
                if (sheetCapture != null) {
                    sheetCapture.setVisibility(View.GONE);
                }
                if ("NO_SCANS".equals(reason)) {
                    View root = getView();
                    if (root != null) {
                        Snackbar.make(root, R.string.capture_discarded_no_scans, Snackbar.LENGTH_LONG).show();
                    }
                }
                loadPoints();
            }

            @Override
            public void onError(String message) {
                planView.clearDraggablePin();
                if (sheetCapture != null) {
                    sheetCapture.setVisibility(View.GONE);
                }
                View root = getView();
                if (root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        captureCoordinator.onSurveyOpened();
    }

    private void loadPoints() {
        if (sessionId == null || getContext() == null) {
            return;
        }

        backgroundExecutor.execute(() -> {
            NetFieldDatabase db = NetFieldDatabase.get(requireContext());
            List<SurveyPointRow> points = db.netFieldDao().getSurveyPointsForSession(sessionId);
            Map<String, Integer> bestRssi = new HashMap<>();

            for (SurveyPointRow pt : points) {
                List<MeasurementRow> measurements = db.netFieldDao().getMeasurementsForPoint(pt.id);
                int maxRssi = -100;
                for (MeasurementRow m : measurements) {
                    if ((int) m.rssiMedian > maxRssi) {
                        maxRssi = (int) m.rssiMedian;
                    }
                }
                bestRssi.put(pt.id, maxRssi);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    surveyPoints.clear();
                    surveyPoints.addAll(points);
                    pointBestRssiMap.clear();
                    pointBestRssiMap.putAll(bestRssi);

                    textPinCount.setText(getString(R.string.survey_pins_format, surveyPoints.size()));
                    if (surveyPoints.isEmpty() && (captureCoordinator == null || !captureCoordinator.isCapturing())) {
                        cardEmptyHint.setVisibility(View.VISIBLE);
                    } else {
                        cardEmptyHint.setVisibility(View.GONE);
                    }
                    planView.invalidate();
                });
            }
        });
    }

    private void openPointDetail(String pointId) {
        Bundle args = new Bundle();
        args.putString("pointId", pointId);
        NavHostFragment.findNavController(this).navigate(R.id.action_s07_to_s08, args);
    }

    private void navigateToHeatmap() {
        Bundle args = new Bundle();
        args.putString("sessionId", sessionId);
        NavHostFragment.findNavController(this).navigate(R.id.action_s07_to_s09, args);
    }

    private void showOverflowMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.inflate(R.menu.menu_survey);
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_end_survey) {
                showEndSurveyConfirm();
                return true;
            } else if (id == R.id.action_edit_geometry) {
                navigateToHeatmap();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showEndSurveyConfirm() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_end_survey_title)
                .setMessage(R.string.dialog_end_survey_message)
                .setPositiveButton(R.string.dialog_end_survey_confirm, (dialog, which) -> {
                    SessionExecutor executor = NetFieldApp.sessionExecutor();
                    if (executor != null) {
                        executor.post(new SessionEvent(SessionEvent.Type.END_SESSION, clock.monoNs()));
                    }
                    SessionRepository repo = SessionRepository.create(requireContext());
                    repo.updateState(sessionId, Sm1State.ENDED.name());
                    boolean popped = NavHostFragment.findNavController(this).popBackStack(R.id.nav_s03, false);
                    if (!popped) {
                        NavHostFragment.findNavController(this).popBackStack();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private List<Integer> parseReferenceFreqs(String refJson) {
        if (refJson == null || refJson.isEmpty() || "[]".equals(refJson)) {
            return Collections.emptyList();
        }
        List<Integer> list = new ArrayList<>();
        String clean = refJson.replace("[", "").replace("]", "").trim();
        if (clean.isEmpty()) {
            return list;
        }
        for (String p : clean.split(",")) {
            try {
                list.add(Integer.parseInt(p.trim()));
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    @androidx.annotation.VisibleForTesting
    public void setCaptureCoordinatorForTest(CaptureCoordinator coordinator) {
        this.captureCoordinator = coordinator;
    }

    @androidx.annotation.VisibleForTesting
    public CaptureCoordinator getCaptureCoordinator() {
        return captureCoordinator;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPoints();
    }
}
