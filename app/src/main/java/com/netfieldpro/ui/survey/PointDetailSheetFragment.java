package com.netfieldpro.ui.survey;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.netfieldpro.R;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.entity.MeasurementRow;
import com.netfieldpro.data.db.entity.SurveyPointRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PointDetailSheetFragment extends BottomSheetDialogFragment {

    private static Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    @androidx.annotation.VisibleForTesting
    public static void setBackgroundExecutorForTest(Executor executor) {
        backgroundExecutor = executor != null ? executor : Executors.newSingleThreadExecutor();
    }

    public interface PointDeleteListener {
        void onPointDeleted(String pointId);
    }

    private View sheetPoint;
    private TextView textPointTitle;
    private Chip chipQuality;
    private RecyclerView listPointBssids;
    private MaterialButton btnPointDelete;

    private PointMeasurementsAdapter adapter;
    private PointDeleteListener deleteListener;
    private String pointId;

    public void setPointDeleteListener(PointDeleteListener listener) {
        this.deleteListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_point, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sheetPoint = view.findViewById(R.id.sheet_point);
        textPointTitle = view.findViewById(R.id.text_point_title);
        chipQuality = view.findViewById(R.id.chip_quality);
        listPointBssids = view.findViewById(R.id.list_point_bssids);
        btnPointDelete = view.findViewById(R.id.btn_point_delete);

        adapter = new PointMeasurementsAdapter();
        listPointBssids.setLayoutManager(new LinearLayoutManager(requireContext()));
        listPointBssids.setAdapter(adapter);

        if (getArguments() != null) {
            pointId = getArguments().getString("pointId");
        }

        loadPointData();
    }

    private void loadPointData() {
        if (pointId == null || getContext() == null) {
            return;
        }

        backgroundExecutor.execute(() -> {
            NetFieldDatabase db = NetFieldDatabase.get(requireContext());
            SurveyPointRow point = db.netFieldDao().getSurveyPoint(pointId);
            List<MeasurementRow> measurements = db.netFieldDao().getMeasurementsForPoint(pointId);
            boolean isFinalized = false;
            if (point != null) {
                SurveySessionRow session = db.netFieldDao().getSurveySession(point.sessionId);
                if (session != null && "FINALIZED".equalsIgnoreCase(session.state)) {
                    isFinalized = true;
                }
            }

            final boolean readOnly = isFinalized;
            final SurveyPointRow finalPoint = point;
            final List<MeasurementRow> finalMeasurements = measurements != null ? measurements : Collections.emptyList();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (finalPoint != null) {
                        textPointTitle.setText(getString(R.string.point_title_format, finalPoint.id.substring(0, Math.min(8, finalPoint.id.length()))));

                        String qualityText;
                        if ("MOVED".equalsIgnoreCase(finalPoint.quality)) {
                            qualityText = getString(R.string.point_quality_moved);
                        } else if ("LOW_CONFIDENCE".equalsIgnoreCase(finalPoint.quality)) {
                            qualityText = getString(R.string.point_quality_low_confidence);
                        } else {
                            qualityText = getString(R.string.point_quality_ok);
                        }
                        chipQuality.setText(qualityText);
                        adapter.setItems(finalMeasurements);

                        if (readOnly) {
                            btnPointDelete.setVisibility(View.GONE);
                        } else {
                            btnPointDelete.setVisibility(View.VISIBLE);
                            btnPointDelete.setOnClickListener(v -> showDeleteConfirm());
                        }
                    }
                });
            }
        });
    }

    private void showDeleteConfirm() {
        if (getContext() == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_point_title)
                .setMessage(R.string.dialog_delete_point_message)
                .setPositiveButton(R.string.dialog_delete_point_confirm, (dialog, which) -> {
                    backgroundExecutor.execute(() -> {
                        NetFieldDatabase.get(requireContext()).netFieldDao().deleteSurveyPoint(pointId);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (deleteListener != null) {
                                    deleteListener.onPointDeleted(pointId);
                                }
                                dismiss();
                            });
                        }
                    });
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }
}
