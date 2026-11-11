package com.netfieldpro.ui.plan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.QuotaDecision;
import com.netfieldpro.core.entitlement.Resource;
import com.netfieldpro.core.model.entity.Floor;
import com.netfieldpro.core.model.enums.ScaleMethod;
import com.netfieldpro.core.project.LevelRules;
import com.netfieldpro.core.util.IdGenerator;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.mapper.FloorMapper;
import com.netfieldpro.data.plan.PlanImporter;
import com.netfieldpro.databinding.FragmentFloorImportBinding;

public class FloorImportFragment extends Fragment {

    private FragmentFloorImportBinding binding;
    private String projectId;
    private String pendingFloorId;
    private PlanImporter.ImportResult currentResult;
    private boolean saved = false;

    private ActivityResultLauncher<String[]> pickDocumentLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pendingFloorId = IdGenerator.generate();

        pickDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::onDocumentPicked
        );
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentFloorImportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            projectId = getArguments().getString("projectId");
        }

        binding.btnPickFile.setOnClickListener(v -> {
            pickDocumentLauncher.launch(new String[]{"image/png", "image/jpeg", "application/pdf"});
        });

        binding.editFloorLevel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateLevelAndSaveState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.btnSave.setOnClickListener(v -> onSaveClicked());
    }

    private void onDocumentPicked(Uri uri) {
        if (uri == null || getContext() == null) {
            return;
        }

        try {
            Context context = requireContext();
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            File tempSource = File.createTempFile("picked_source_", ".tmp", context.getCacheDir());
            try (InputStream in = context.getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(tempSource)) {
                if (in == null) return;
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) {
                    out.write(buf, 0, r);
                }
                out.flush();
            }

            onFilePicked(tempSource, mimeType);
            tempSource.delete();
        } catch (Exception e) {
            showError(R.string.import_err_decode);
        }
    }

    @VisibleForTesting
    public void onFilePicked(@NonNull File file, @NonNull String mimeType) {
        if (binding == null) return;

        File plansDir = NetFieldApp.planFileStore().getPlansDir();
        PlanImporter.ImportResult result = PlanImporter.importFile(
                requireContext(),
                file,
                mimeType,
                plansDir,
                pendingFloorId
        );

        if (!result.success()) {
            currentResult = null;
            showError(result.errorStringRes());
        } else {
            currentResult = result;
            showSuccess(result);
        }
    }

    private void showError(int errorRes) {
        if (binding == null) return;
        binding.textError.setText(errorRes);
        binding.textError.setVisibility(View.VISIBLE);
        binding.layoutPreviewContainer.setVisibility(View.GONE);
        binding.editFloorName.setEnabled(false);
        binding.editFloorLevel.setEnabled(false);
        binding.btnSave.setEnabled(false);
    }

    private void showSuccess(PlanImporter.ImportResult result) {
        if (binding == null) return;
        binding.textError.setVisibility(View.GONE);
        binding.layoutPreviewContainer.setVisibility(View.VISIBLE);
        if (result.storedFile() != null) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            int maxSide = Math.max(result.width(), result.height());
            int sampleSize = 1;
            while (maxSide / sampleSize > 2048) {
                sampleSize *= 2;
            }
            opts.inSampleSize = sampleSize;
            Bitmap bmp = BitmapFactory.decodeFile(result.storedFile().getAbsolutePath(), opts);
            if (bmp != null) {
                binding.planPreview.setOverviewBitmap(bmp, result.width(), result.height());
            }
        }

        binding.editFloorName.setEnabled(true);
        binding.editFloorLevel.setEnabled(true);

        long defaultLevel = 0;
        if (projectId != null) {
            defaultLevel = NetFieldApp.floorRepository().getNextDefaultLevel(projectId);
        }
        binding.editFloorLevel.setText(String.valueOf(defaultLevel));
        binding.editFloorName.setText(getString(R.string.floor_level_label, defaultLevel));

        validateLevelAndSaveState();
    }

    private void validateLevelAndSaveState() {
        if (binding == null) return;
        String levelStr = binding.editFloorLevel.getText() != null
                ? binding.editFloorLevel.getText().toString().trim()
                : "";

        boolean levelValid = false;
        try {
            long level = Long.parseLong(levelStr);
            if (LevelRules.isValid(level)) {
                levelValid = true;
                binding.layoutFloorLevel.setError(null);
            } else {
                binding.layoutFloorLevel.setError(getString(R.string.import_err_level));
            }
        } catch (NumberFormatException e) {
            binding.layoutFloorLevel.setError(getString(R.string.import_err_level));
        }

        boolean canSave = levelValid && currentResult != null && currentResult.success();
        binding.btnSave.setEnabled(canSave);
    }

    private void onSaveClicked() {
        QuotaDecision decision = NetFieldApp.quotaGuard().check(Resource.FLOOR);
        if (!decision.allowed()) {
            if (currentResult != null && currentResult.storedFile() != null) {
                currentResult.storedFile().delete();
            }
            NetFieldApp.planFileStore().deletePlanFile(pendingFloorId);

            Bundle args = new Bundle();
            args.putString("quota", Resource.FLOOR.name());
            NavController navController = Navigation.findNavController(requireView());
            try {
                navController.navigate(R.id.action_global_paywall, args);
            } catch (Exception e) {
                navController.navigate(R.id.nav_s18, args);
            }
            return;
        }

        long level = 0;
        try {
            level = Long.parseLong(binding.editFloorLevel.getText().toString().trim());
        } catch (Exception ignored) {
        }

        String ext = currentResult.extension() != null ? currentResult.extension() : "png";
        File finalFile = NetFieldApp.planFileStore().getPlanFile(pendingFloorId, ext);
        if (currentResult.storedFile() != null && currentResult.storedFile().exists()) {
            if (!currentResult.storedFile().renameTo(finalFile)) {
                try (InputStream in = new java.io.FileInputStream(currentResult.storedFile());
                     OutputStream out = new java.io.FileOutputStream(finalFile)) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = in.read(buf)) != -1) {
                        out.write(buf, 0, r);
                    }
                    out.flush();
                } catch (Exception ignored) {
                }
                currentResult.storedFile().delete();
            }
        }

        Floor floor = new Floor(
                pendingFloorId,
                projectId != null ? projectId : IdGenerator.generate(),
                level,
                currentResult.sha256(),
                currentResult.width(),
                currentResult.height(),
                1.0,
                null,
                ScaleMethod.LINE,
                0.0,
                false,
                null,
                null,
                null,
                null,
                null
        );

        NetFieldApp.floorRepository().createFloor(floor, finalFile);

        saved = true;

        Bundle args = new Bundle();
        args.putString("floorId", pendingFloorId);
        Navigation.findNavController(requireView()).navigate(R.id.action_s04_to_s05, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (!saved && currentResult != null && currentResult.storedFile() != null) {
            currentResult.storedFile().delete();
            NetFieldApp.planFileStore().deletePlanFile(pendingFloorId);
        }
        binding = null;
    }
}
