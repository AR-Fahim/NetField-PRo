package com.netfieldpro.ui.debug;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.netfieldpro.BuildConfig;
import com.netfieldpro.R;
import com.netfieldpro.databinding.FragmentDebugMenuBinding;
import com.netfieldpro.ui.placeholder.PlaceholderSpec;

public class DebugMenuFragment extends Fragment {

    private FragmentDebugMenuBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDebugMenuBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        com.netfieldpro.settings.AppSettings settings = com.netfieldpro.settings.AppSettings.get(requireContext());
        com.netfieldpro.core.settings.ThemeMode currentTheme = settings.getTheme();
        if (currentTheme == com.netfieldpro.core.settings.ThemeMode.LIGHT) {
            binding.radioThemeLight.setChecked(true);
        } else if (currentTheme == com.netfieldpro.core.settings.ThemeMode.DARK) {
            binding.radioThemeDark.setChecked(true);
        } else {
            binding.radioThemeSystem.setChecked(true);
        }

        binding.radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            com.netfieldpro.core.settings.ThemeMode mode;
            if (checkedId == R.id.radio_theme_light) {
                mode = com.netfieldpro.core.settings.ThemeMode.LIGHT;
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (checkedId == R.id.radio_theme_dark) {
                mode = com.netfieldpro.core.settings.ThemeMode.DARK;
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                mode = com.netfieldpro.core.settings.ThemeMode.SYSTEM;
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
            settings.setTheme(mode);
        });

        binding.btnStyleGuide.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.nav_style_guide)
        );

        binding.btnDataLab.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.nav_debug_data_lab)
        );

        binding.btnGateLab.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.nav_debug_gate_lab)
        );

        binding.btnPlanLab.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.nav_debug_plan_lab)
        );

        binding.btnWalkAll.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.nav_s02)
        );

        int resetRowId = getResources().getIdentifier(
                "row_debug_" + "reset_onboarding", "id", requireContext().getPackageName()
        );
        if (resetRowId != 0) {
            View resetRow = view.findViewById(resetRowId);
            if (resetRow != null) {
                resetRow.setOnClickListener(v -> {
                    com.netfieldpro.NetFieldApp.onboardingStore().reset();
                    Intent intent = new Intent(requireContext(), com.netfieldpro.MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                });
            }
        }

        String buildInfo = "Version: " + BuildConfig.VERSION_NAME +
                " | Git: dev | SDK: " + Build.VERSION.SDK_INT;
        binding.textBuildInfo.setText(buildInfo);

        setupScreensList();
    }

    private void setupScreensList() {
        binding.layoutScreensList.removeAllViews();

        MaterialButton s00Btn = createScreenButton("S00", "Auth Slot (S00)", R.id.nav_auth_slot);
        binding.layoutScreensList.addView(s00Btn);

        for (PlaceholderSpec spec : PlaceholderSpec.all()) {
            String title = getString(spec.getTitleRes());
            MaterialButton btn = createScreenButton(spec.getId(), title, spec.getDestinationId());
            binding.layoutScreensList.addView(btn);
        }
    }

    private MaterialButton createScreenButton(String id, String title, int destinationId) {
        MaterialButton btn = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.touch_primary)
        );
        lp.topMargin = getResources().getDimensionPixelSize(R.dimen.space_1);
        btn.setLayoutParams(lp);
        String label = getString(R.string.edge_button_format, id, title);
        btn.setText(label);
        btn.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(destinationId));
        return btn;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

