package com.netfieldpro.ui.debug;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.netfieldpro.databinding.FragmentPlanViewLabBinding;

public class PlanViewLabFragment extends Fragment {

    private FragmentPlanViewLabBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentPlanViewLabBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            File cacheFile = new File(requireContext().getCacheDir(), "sample_plan.png");
            if (!cacheFile.exists()) {
                try (InputStream in = requireContext().getAssets().open("sample_plan.png");
                     OutputStream out = new FileOutputStream(cacheFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
            }
            binding.planView.loadPlanFile(cacheFile);
            binding.planView.setHandles(500, 500, 1500, 500);
        } catch (Exception e) {
            // Fallback for environments without assets
            Bitmap bmp = Bitmap.createBitmap(2000, 1000, Bitmap.Config.ARGB_8888);
            binding.planView.setOverviewBitmap(bmp, 2000, 1000);
            binding.planView.setHandles(500, 500, 1500, 500);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
