package com.netfieldpro.ui.debug;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.DecisionKind;
import com.netfieldpro.core.entitlement.EntitlementDecision;
import com.netfieldpro.core.entitlement.EntitlementPolicy;
import com.netfieldpro.core.entitlement.Feature;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.paywall.PaywallGate;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DebugGateLabFragment extends Fragment {

    private RadioButton rbFree;
    private RadioButton rbPro;
    private RadioButton rbEnterprise;
    private LinearLayout layoutFeatureRows;
    private final Map<Feature, TextView> badgeViews = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_debug_gate_lab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rbFree = view.findViewById(R.id.rb_gatelab_free);
        rbPro = view.findViewById(R.id.rb_gatelab_pro);
        rbEnterprise = view.findViewById(R.id.rb_gatelab_enterprise);
        layoutFeatureRows = view.findViewById(R.id.layout_feature_rows);

        setupTierRadios();
        buildFeatureRows();

        NetFieldApp.entitlements().tier().observe(getViewLifecycleOwner(), tier -> {
            if (tier != null) {
                syncTierRadio(tier);
                updateAllBadges(tier);
            }
        });
    }

    private void setupTierRadios() {
        Tier current = NetFieldApp.entitlements().current();
        syncTierRadio(current);

        rbFree.setOnClickListener(v -> NetFieldApp.entitlements().setDebugTier(Tier.FREE));
        rbPro.setOnClickListener(v -> NetFieldApp.entitlements().setDebugTier(Tier.PRO));
        rbEnterprise.setOnClickListener(v -> NetFieldApp.entitlements().setDebugTier(Tier.ENTERPRISE));
    }

    private void syncTierRadio(Tier tier) {
        rbFree.setChecked(tier == Tier.FREE);
        rbPro.setChecked(tier == Tier.PRO);
        rbEnterprise.setChecked(tier == Tier.ENTERPRISE);
    }

    private void buildFeatureRows() {
        layoutFeatureRows.removeAllViews();
        badgeViews.clear();

        Tier currentTier = NetFieldApp.entitlements().current();

        for (Feature f : Feature.values()) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 12, 0, 12);

            String title = getFeatureTitle(f);

            TextView textTitle = new TextView(requireContext());
            textTitle.setText(title);
            textTitle.setTextAppearance(R.style.TextAppearance_NF_Body);
            textTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.nf_on_surface));
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textTitle.setLayoutParams(titleLp);
            row.addView(textTitle);

            TextView textBadge = new TextView(requireContext());
            textBadge.setTextAppearance(R.style.TextAppearance_NF_Label);
            textBadge.setTag("badge_" + f.name());
            textBadge.setPadding(16, 0, 16, 0);
            row.addView(textBadge);
            badgeViews.put(f, textBadge);

            MaterialButton btnTry = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btnTry.setText(R.string.gatelab_try);
            btnTry.setTag("try_" + f.name());
            btnTry.setOnClickListener(v -> {
                PaywallGate.require(this, f, () -> {
                    if (getView() != null) {
                        Snackbar.make(requireView(), getString(R.string.gatelab_try_result, title), Snackbar.LENGTH_SHORT).show();
                    }
                });
            });
            row.addView(btnTry);

            layoutFeatureRows.addView(row);
        }

        updateAllBadges(currentTier);
    }

    private void updateAllBadges(Tier tier) {
        for (Feature f : Feature.values()) {
            TextView badge = badgeViews.get(f);
            if (badge == null) continue;

            EntitlementDecision decision = EntitlementPolicy.decide(tier, f);
            if (decision.kind() == DecisionKind.ALLOWED) {
                badge.setText(R.string.gatelab_allowed);
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.nf_primary));
            } else if (decision.kind() == DecisionKind.LOCKED) {
                if (decision.requiredTier() == Tier.ENTERPRISE) {
                    badge.setText(R.string.badge_enterprise);
                } else {
                    badge.setText(R.string.badge_pro);
                }
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.nf_primary));
            } else {
                badge.setText(R.string.gatelab_na);
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.nf_on_surface_variant));
            }
        }
    }

    private String getFeatureTitle(Feature f) {
        int resId = requireContext().getResources().getIdentifier(
                "feature_" + f.name().toLowerCase(Locale.ROOT),
                "string",
                requireContext().getPackageName());
        if (resId != 0) {
            return getString(resId);
        }
        return f.name();
    }
}

