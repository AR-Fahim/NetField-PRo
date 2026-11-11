package com.netfieldpro.ui.debug;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.netfieldpro.R;
import com.netfieldpro.databinding.FragmentStyleGuideBinding;

public class StyleGuideFragment extends Fragment {

    private FragmentStyleGuideBinding binding;

    private static class Swatch {
        final String name;
        final int colorRes;

        Swatch(String name, int colorRes) {
            this.name = name;
            this.colorRes = colorRes;
        }
    }

    private static final Swatch[] SWATCHES = new Swatch[]{
            new Swatch("nf_primary", R.color.nf_primary),
            new Swatch("nf_on_primary", R.color.nf_on_primary),
            new Swatch("nf_primary_container", R.color.nf_primary_container),
            new Swatch("nf_background", R.color.nf_background),
            new Swatch("nf_surface", R.color.nf_surface),
            new Swatch("nf_on_surface", R.color.nf_on_surface),
            new Swatch("nf_on_surface_variant", R.color.nf_on_surface_variant),
            new Swatch("nf_outline", R.color.nf_outline),
            new Swatch("nf_error", R.color.nf_error),
            new Swatch("nf_warning", R.color.nf_warning),
            new Swatch("nf_info", R.color.nf_info)
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStyleGuideBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.layoutColorSwatches.removeAllViews();
        for (Swatch swatch : SWATCHES) {
            int resolvedColor = ContextCompat.getColor(requireContext(), swatch.colorRes);
            String hex = String.format("#%06X", (0xFFFFFF & resolvedColor));

            LinearLayout swatchRow = new LinearLayout(requireContext());
            swatchRow.setOrientation(LinearLayout.HORIZONTAL);
            swatchRow.setPadding(0, getResources().getDimensionPixelSize(R.dimen.space_050), 0, getResources().getDimensionPixelSize(R.dimen.space_050));

            View colorBox = new View(requireContext());
            LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.touch_min),
                    getResources().getDimensionPixelSize(R.dimen.touch_min)
            );
            colorBox.setLayoutParams(boxLp);

            GradientDrawable gd = new GradientDrawable();
            gd.setColor(resolvedColor);
            gd.setCornerRadius(getResources().getDimensionPixelSize(R.dimen.radius_chip));
            gd.setStroke(getResources().getDimensionPixelSize(R.dimen.stroke),
                    ContextCompat.getColor(requireContext(), R.color.nf_outline));
            colorBox.setBackground(gd);

            TextView label = new TextView(requireContext());
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            labelLp.setMarginStart(getResources().getDimensionPixelSize(R.dimen.space_2));
            labelLp.gravity = android.view.Gravity.CENTER_VERTICAL;
            label.setLayoutParams(labelLp);
            label.setTextAppearance(R.style.TextAppearance_NF_Body);
            label.setText(swatch.name + " (" + hex + ")");

            swatchRow.addView(colorBox);
            swatchRow.addView(label);

            binding.layoutColorSwatches.addView(swatchRow);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
