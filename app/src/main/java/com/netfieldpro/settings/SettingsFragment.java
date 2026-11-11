package com.netfieldpro.settings;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.netfieldpro.BuildConfig;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.DecisionKind;
import com.netfieldpro.core.entitlement.EntitlementPolicy;
import com.netfieldpro.core.entitlement.Feature;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.core.settings.HeatPalette;
import com.netfieldpro.core.settings.ThemeMode;
import com.netfieldpro.databinding.FragmentSettingsBinding;
import com.netfieldpro.paywall.PaywallGate;
import com.netfieldpro.ui.settings.SettingsExtensionRegistry;
import com.netfieldpro.ui.settings.SettingsRow;

import java.util.List;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupPlanSection();
        setupAppearanceSection();
        setupDeviceSection();
        setupProfilesSection();
        setupAboutSection();
        setupExtensionsSection();
    }

    private void setupPlanSection() {
        NetFieldApp.entitlements().tier().observe(getViewLifecycleOwner(), tier -> {
            if (binding != null && tier != null) {
                binding.tierBadgeView.setTier(tier);
                updateProfilesLockBadge(tier);
            }
        });

        binding.rowManagePlan.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_s19_to_s18, null);
        });
    }

    private void setupAppearanceSection() {
        AppSettings settings = AppSettings.get(requireContext());

        // Theme
        ThemeMode currentTheme = settings.getTheme();
        if (currentTheme == ThemeMode.LIGHT) {
            binding.rbThemeLight.setChecked(true);
        } else if (currentTheme == ThemeMode.DARK) {
            binding.rbThemeDark.setChecked(true);
        } else {
            binding.rbThemeSystem.setChecked(true);
        }

        binding.rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            ThemeMode mode;
            if (checkedId == R.id.rb_theme_light) {
                mode = ThemeMode.LIGHT;
            } else if (checkedId == R.id.rb_theme_dark) {
                mode = ThemeMode.DARK;
            } else {
                mode = ThemeMode.SYSTEM;
            }
            settings.setTheme(mode);
            int nightMode;
            if (mode == ThemeMode.LIGHT) {
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (mode == ThemeMode.DARK) {
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
            } else {
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }
            AppCompatDelegate.setDefaultNightMode(nightMode);
        });

        // Heatmap Palette
        HeatPalette currentPalette = settings.getHeatPalette();
        if (currentPalette == HeatPalette.VIRIDIS) {
            binding.rbPaletteViridis.setChecked(true);
            binding.rbPaletteTraffic.setChecked(false);
        } else {
            binding.rbPaletteTraffic.setChecked(true);
            binding.rbPaletteViridis.setChecked(false);
        }

        binding.rbPaletteTraffic.setOnClickListener(v -> {
            binding.rbPaletteTraffic.setChecked(true);
            binding.rbPaletteViridis.setChecked(false);
            settings.setHeatPalette(HeatPalette.TRAFFIC_LIGHT);
        });

        binding.rbPaletteViridis.setOnClickListener(v -> {
            binding.rbPaletteViridis.setChecked(true);
            binding.rbPaletteTraffic.setChecked(false);
            settings.setHeatPalette(HeatPalette.VIRIDIS);
        });

        binding.rgPalette.setOnCheckedChangeListener((group, checkedId) -> {
            HeatPalette palette = (checkedId == R.id.rb_palette_viridis)
                    ? HeatPalette.VIRIDIS
                    : HeatPalette.TRAFFIC_LIGHT;
            settings.setHeatPalette(palette);
        });

        // Swatch strips
        applySwatch(binding.swatchTl1, R.color.nf_heat_tl_1);
        applySwatch(binding.swatchTl2, R.color.nf_heat_tl_2);
        applySwatch(binding.swatchTl3, R.color.nf_heat_tl_3);
        applySwatch(binding.swatchTl4, R.color.nf_heat_tl_4);
        applySwatch(binding.swatchTl5, R.color.nf_heat_tl_5);

        applySwatch(binding.swatchVi1, R.color.nf_heat_vi_1);
        applySwatch(binding.swatchVi2, R.color.nf_heat_vi_2);
        applySwatch(binding.swatchVi3, R.color.nf_heat_vi_3);
        applySwatch(binding.swatchVi4, R.color.nf_heat_vi_4);
        applySwatch(binding.swatchVi5, R.color.nf_heat_vi_5);
    }

    private void applySwatch(View v, int colorRes) {
        float density = getResources().getDisplayMetrics().density;
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(2 * density);
        gd.setColor(ContextCompat.getColor(requireContext(), colorRes));
        v.setBackground(gd);
    }

    private void setupDeviceSection() {
        binding.rowDiagnostics.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_s19_to_s20);
        });
    }

    private void setupProfilesSection() {
        Tier currentTier = NetFieldApp.entitlements().current();
        updateProfilesLockBadge(currentTier);

        binding.rowCustomProfiles.setOnClickListener(v -> {
            PaywallGate.require(this, Feature.CUSTOM_PROFILES, () -> {
                NavHostFragment.findNavController(this).navigate(R.id.action_s19_to_s21, null);
            });
        });
    }

    private void updateProfilesLockBadge(Tier tier) {
        if (binding == null) return;
        boolean locked = EntitlementPolicy.decide(tier, Feature.CUSTOM_PROFILES).kind() == DecisionKind.LOCKED;
        binding.badgeCustomProfilesLock.setVisibility(locked ? View.VISIBLE : View.GONE);
    }

    private void setupAboutSection() {
        String versionText = BuildConfig.VERSION_NAME + (BuildConfig.DEBUG ? " (DEBUG)" : "");
        binding.textVersion.setText(versionText);
    }

    private void setupExtensionsSection() {
        com.netfieldpro.ui.auth.AuthSettingsExtension.setup(this);
        List<SettingsRow> allRows = SettingsExtensionRegistry.getInstance().rows();
        List<SettingsRow> visibleRows = new java.util.ArrayList<>();
        for (SettingsRow r : allRows) {
            if (r.isVisible()) {
                visibleRows.add(r);
            }
        }

        if (visibleRows.isEmpty()) {
            binding.layoutExtensions.setVisibility(View.GONE);
        } else {
            binding.layoutExtensions.setVisibility(View.VISIBLE);
            binding.layoutExtensionRows.removeAllViews();
            for (SettingsRow row : visibleRows) {
                LinearLayout rowLayout = new LinearLayout(requireContext());
                rowLayout.setOrientation(LinearLayout.VERTICAL);
                rowLayout.setPadding(0, getResources().getDimensionPixelSize(R.dimen.space_1), 0, getResources().getDimensionPixelSize(R.dimen.space_1));

                TextView titleView = new TextView(requireContext());
                titleView.setTextAppearance(R.style.TextAppearance_NF_Body);
                titleView.setText(row.getTitle());
                rowLayout.addView(titleView);

                if (row.getSubtitle() != null && !row.getSubtitle().isEmpty()) {
                    TextView subView = new TextView(requireContext());
                    subView.setTextAppearance(R.style.TextAppearance_NF_Caption);
                    subView.setText(row.getSubtitle());
                    rowLayout.addView(subView);
                }

                if (row.getOnClick() != null) {
                    rowLayout.setOnClickListener(v -> row.getOnClick().run());
                }

                binding.layoutExtensionRows.addView(rowLayout);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
