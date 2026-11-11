package com.netfieldpro.ui.plan;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.io.File;
import java.util.Locale;

import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.model.entity.Floor;
import com.netfieldpro.core.model.enums.ScaleMethod;
import com.netfieldpro.core.scale.ScaleCalculator;
import com.netfieldpro.databinding.FragmentScaleBinding;

public class ScaleFragment extends Fragment {

    private FragmentScaleBinding binding;
    private String floorId;
    private Floor floor;
    private int imageW = 2000;
    private int imageH = 1000;
    private boolean isLocked = false;
    private ScaleCalculator.ScaleResult currentResult;

    private boolean updatingManualCoordinates = false;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentScaleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            floorId = getArguments().getString("floorId");
        }

        if (floorId != null) {
            floor = NetFieldApp.floorRepository().getFloor(floorId);
            isLocked = NetFieldApp.floorRepository().isRecalibrationBlocked(floorId);
        }

        if (isLocked) {
            binding.textScaleLocked.setVisibility(View.VISIBLE);
            binding.btnSaveScale.setEnabled(false);
        } else {
            binding.textScaleLocked.setVisibility(View.GONE);
        }

        if (floor != null && floor.planWpx() > 0 && floor.planHpx() > 0) {
            imageW = floor.planWpx();
            imageH = floor.planHpx();
            File planFile = NetFieldApp.planFileStore().findPlanFile(floorId);
            if (planFile != null && planFile.exists()) {
                binding.planView.loadPlanFile(planFile);
            } else {
                binding.planView.setImageDimensions(imageW, imageH);
            }
        } else {
            binding.planView.setImageDimensions(imageW, imageH);
        }

        // Initial handles: 25% and 75% of width, centered vertically (P05-1)
        double initH1X = 0.25 * imageW;
        double initH1Y = 0.50 * imageH;
        double initH2X = 0.75 * imageW;
        double initH2Y = 0.50 * imageH;
        binding.planView.setHandles(initH1X, initH1Y, initH2X, initH2Y);
        updateManualCoordinateFields(initH1X, initH1Y, initH2X, initH2Y);

        binding.btnSaveScale.setEnabled(false);

        binding.planView.setHandleMoveListener((handleIndex, x, y) -> {
            updateManualCoordinateFields(
                    binding.planView.getHandle1X(),
                    binding.planView.getHandle1Y(),
                    binding.planView.getHandle2X(),
                    binding.planView.getHandle2Y()
            );
            recalculateScale();
        });

        binding.groupScaleMode.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isLine = checkedId == R.id.radio_mode_line;
            if (isLine) {
                binding.planView.setHandles(
                        binding.planView.getHandle1X(),
                        binding.planView.getHandle1Y(),
                        binding.planView.getHandle2X(),
                        binding.planView.getHandle2Y()
                );
                binding.btnManualPoints.setVisibility(View.VISIBLE);
            } else {
                binding.planView.clearHandles();
                binding.btnManualPoints.setVisibility(View.GONE);
                binding.layoutManualPoints.setVisibility(View.GONE);
            }
            recalculateScale();
        });

        binding.btnManualPoints.setOnClickListener(v -> {
            int currentVis = binding.layoutManualPoints.getVisibility();
            binding.layoutManualPoints.setVisibility(currentVis == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        setupManualInputWatchers();

        binding.editDistance.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateScale();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.btnSaveScale.setOnClickListener(v -> onSaveClicked());
    }

    private void setupManualInputWatchers() {
        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (updatingManualCoordinates) return;
                try {
                    double p1x = Double.parseDouble(binding.editP1X.getText().toString());
                    double p1y = Double.parseDouble(binding.editP1Y.getText().toString());
                    double p2x = Double.parseDouble(binding.editP2X.getText().toString());
                    double p2y = Double.parseDouble(binding.editP2Y.getText().toString());
                    binding.planView.setHandles(p1x, p1y, p2x, p2y);
                    recalculateScale();
                } catch (Exception ignored) {
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        binding.editP1X.addTextChangedListener(tw);
        binding.editP1Y.addTextChangedListener(tw);
        binding.editP2X.addTextChangedListener(tw);
        binding.editP2Y.addTextChangedListener(tw);
    }

    private void updateManualCoordinateFields(double h1x, double h1y, double h2x, double h2y) {
        updatingManualCoordinates = true;
        if (binding.editP1X != null) binding.editP1X.setText(String.format(Locale.US, "%.1f", h1x));
        if (binding.editP1Y != null) binding.editP1Y.setText(String.format(Locale.US, "%.1f", h1y));
        if (binding.editP2X != null) binding.editP2X.setText(String.format(Locale.US, "%.1f", h2x));
        if (binding.editP2Y != null) binding.editP2Y.setText(String.format(Locale.US, "%.1f", h2y));
        updatingManualCoordinates = false;
    }

    @VisibleForTesting
    public void recalculateScale() {
        if (binding == null) return;

        if (isLocked) {
            binding.btnSaveScale.setEnabled(false);
        }

        String distStr = binding.editDistance.getText() != null
                ? binding.editDistance.getText().toString().trim()
                : "";

        if (distStr.isEmpty()) {
            binding.textScaleError.setVisibility(View.GONE);
            binding.textScaleReadout.setVisibility(View.GONE);
            binding.btnSaveScale.setEnabled(false);
            currentResult = null;
            return;
        }

        double distanceM;
        try {
            distanceM = Double.parseDouble(distStr);
        } catch (NumberFormatException e) {
            binding.textScaleError.setText(R.string.scale_err_distance);
            binding.textScaleError.setVisibility(View.VISIBLE);
            binding.textScaleReadout.setVisibility(View.GONE);
            binding.btnSaveScale.setEnabled(false);
            currentResult = null;
            return;
        }

        if (ScaleCalculator.validateDistance(distanceM) != ScaleCalculator.ValidationStatus.OK) {
            binding.textScaleError.setText(R.string.scale_err_distance);
            binding.textScaleError.setVisibility(View.VISIBLE);
            binding.textScaleReadout.setVisibility(View.GONE);
            binding.btnSaveScale.setEnabled(false);
            currentResult = null;
            return;
        }

        boolean isLineMode = binding.radioModeLine.isChecked();

        if (isLineMode) {
            double x1 = binding.planView.getHandle1X();
            double y1 = binding.planView.getHandle1Y();
            double x2 = binding.planView.getHandle2X();
            double y2 = binding.planView.getHandle2Y();

            if (ScaleCalculator.validateLineSpan(x1, y1, x2, y2, imageW, imageH) == ScaleCalculator.ValidationStatus.TOO_SHORT) {
                binding.textScaleError.setText(R.string.scale_err_short);
                binding.textScaleError.setVisibility(View.VISIBLE);
                binding.textScaleReadout.setVisibility(View.GONE);
                binding.btnSaveScale.setEnabled(false);
                currentResult = null;
                return;
            }

            ScaleCalculator.ScaleResult res = ScaleCalculator.calculateLine(x1, y1, x2, y2, distanceM, imageW, imageH);
            showReadout(res);
        } else {
            ScaleCalculator.ScaleResult res = ScaleCalculator.calculateRoomWidth(distanceM, imageW);
            showReadout(res);
        }
    }

    private void showReadout(ScaleCalculator.ScaleResult res) {
        currentResult = res;
        binding.textScaleError.setVisibility(View.GONE);

        String template = getString(R.string.scale_readout);
        String text = template.replace("%1$s", res.mppFormatted()).replace("%2$s", res.uncertaintyFormatted());
        binding.textScaleReadout.setText(text);
        binding.textScaleReadout.setVisibility(View.VISIBLE);

        if (!isLocked) {
            binding.btnSaveScale.setEnabled(true);
        }
    }

    private void onSaveClicked() {
        if (currentResult == null || floorId == null || isLocked) {
            return;
        }

        boolean isLineMode = binding.radioModeLine.isChecked();
        Floor.Point p1 = isLineMode ? new Floor.Point(binding.planView.getHandle1X(), binding.planView.getHandle1Y()) : null;
        Floor.Point p2 = isLineMode ? new Floor.Point(binding.planView.getHandle2X(), binding.planView.getHandle2Y()) : null;
        ScaleMethod method = isLineMode ? ScaleMethod.LINE : ScaleMethod.ROOM_WIDTH;

        NetFieldApp.floorRepository().updateScale(
                floorId,
                currentResult.metersPerPixel(),
                currentResult.uncertaintyPct(),
                method,
                true,
                p1,
                p2
        );

        Navigation.findNavController(requireView()).popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
