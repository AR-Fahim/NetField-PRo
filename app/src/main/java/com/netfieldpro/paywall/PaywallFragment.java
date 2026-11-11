package com.netfieldpro.paywall;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.netfieldpro.BuildConfig;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.DecisionKind;
import com.netfieldpro.core.entitlement.EntitlementDecision;
import com.netfieldpro.core.entitlement.EntitlementPolicy;
import com.netfieldpro.core.entitlement.Feature;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.databinding.FragmentPaywallBinding;
import com.netfieldpro.ui.NavArgs;

import java.util.Locale;

public class PaywallFragment extends Fragment {

    private static final Feature[] REPRESENTATIVE_FEATURES = new Feature[] {
            Feature.TAP_CAPTURE,
            Feature.VERIFY_REPORT,
            Feature.FINALIZE_SIGN,
            Feature.PRESET_OFFICE,
            Feature.ROAMING_AUDIT,
            Feature.PRESET_WAREHOUSE
    };

    private static final int[] REPRESENTATIVE_TITLES = new int[] {
            R.string.paywall_row_capture,
            R.string.paywall_row_verify,
            R.string.paywall_row_sign,
            R.string.paywall_row_presets,
            R.string.paywall_row_roaming,
            R.string.paywall_row_warehouse
    };

    private FragmentPaywallBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle initialResult = new Bundle();
        initialResult.putString(NavArgs.RESULT_VALUE, NavArgs.RESULT_DISMISSED);
        getParentFragmentManager().setFragmentResult(NavArgs.RESULT_KEY_PAYWALL, initialResult);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPaywallBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Feature targetFeature = null;
        if (getArguments() != null) {
            String featureStr = getArguments().getString(NavArgs.ARG_FEATURE);
            if (featureStr == null) {
                featureStr = getArguments().getString(NavArgs.FEATURE);
            }
            if (featureStr != null) {
                try {
                    targetFeature = Feature.valueOf(featureStr);
                } catch (IllegalArgumentException ignored) {
                    targetFeature = null;
                }
            }
        }

        populateComparisonTable();
        bindUI(targetFeature);
    }

    private void bindUI(Feature targetFeature) {
        Tier currentTier = NetFieldApp.entitlements().current();

        if (targetFeature == null) {
            setupChoosePlanState();
            return;
        }

        EntitlementDecision decision = EntitlementPolicy.decide(currentTier, targetFeature);
        if (decision.kind() == DecisionKind.LOCKED) {
            setupLockedState(targetFeature, decision.requiredTier(), currentTier);
        } else if (decision.kind() == DecisionKind.ALLOWED) {
            setupStaleState(targetFeature);
        } else {
            setupChoosePlanState();
        }
    }

    private void setupLockedState(Feature feature, Tier requiredTier, Tier currentTier) {
        binding.textLockBadge.setVisibility(View.VISIBLE);
        if (requiredTier == Tier.ENTERPRISE) {
            binding.textLockBadge.setText(R.string.badge_enterprise);
        } else {
            binding.textLockBadge.setText(R.string.badge_pro);
        }

        int titleRes = getContext().getResources().getIdentifier(
                "feature_" + feature.name().toLowerCase(Locale.ROOT),
                "string",
                getContext().getPackageName());
        if (titleRes != 0) {
            binding.textScreenTitle.setText(titleRes);
        } else {
            binding.textScreenTitle.setText(feature.name());
        }

        String reqStr = getString(R.string.lock_requires, tierDisplayName(requiredTier));
        String curStr = getString(R.string.paywall_your_plan, tierDisplayName(currentTier));
        binding.textPaywallSubtitle.setVisibility(View.VISIBLE);
        binding.textPaywallSubtitle.setText(reqStr + ". " + curStr);

        binding.textPaywallIncluded.setVisibility(View.GONE);
        binding.btnContinue.setVisibility(View.GONE);
        binding.btnCta.setVisibility(View.VISIBLE);
        binding.btnCta.setEnabled(false);

        if (BuildConfig.DEBUG && requiredTier != null) {
            binding.btnDevSimulate.setVisibility(View.VISIBLE);
            int resId = getContext().getResources().getIdentifier(
                    "paywall_dev_simulate", "string", getContext().getPackageName());
            if (resId != 0) {
                binding.btnDevSimulate.setText(getString(resId, tierDisplayName(requiredTier)));
            }
            binding.btnDevSimulate.setOnClickListener(v -> {
                NetFieldApp.entitlements().setDebugTier(requiredTier);
                Bundle res = new Bundle();
                res.putString(NavArgs.RESULT_VALUE, NavArgs.RESULT_PURCHASED);
                getParentFragmentManager().setFragmentResult(NavArgs.RESULT_KEY_PAYWALL, res);
                NavHostFragment.findNavController(this).popBackStack();
            });
        } else {
            binding.btnDevSimulate.setVisibility(View.GONE);
        }

        binding.btnNotNow.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
    }

    private void setupChoosePlanState() {
        binding.textLockBadge.setVisibility(View.GONE);
        binding.textScreenTitle.setText(R.string.paywall_choose_plan);
        binding.textPaywallIncluded.setVisibility(View.GONE);
        binding.btnContinue.setVisibility(View.GONE);
        binding.btnDevSimulate.setVisibility(View.GONE);
        binding.btnCta.setVisibility(View.VISIBLE);
        binding.btnCta.setEnabled(false);
        binding.btnNotNow.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        String quota = null;
        if (getArguments() != null) {
            quota = getArguments().getString(NavArgs.QUOTA);
        }
        if (quota != null) {
            int quotaResId = 0;
            String upper = quota.toUpperCase(Locale.ROOT);
            if ("PROJECT".equals(upper)) {
                quotaResId = R.string.paywall_quota_project;
            } else if ("FLOOR".equals(upper)) {
                quotaResId = R.string.paywall_quota_floor;
            } else if ("SESSION".equals(upper)) {
                quotaResId = R.string.paywall_quota_session;
            }
            if (quotaResId != 0) {
                binding.textPaywallSubtitle.setVisibility(View.VISIBLE);
                binding.textPaywallSubtitle.setText(quotaResId);
            } else {
                binding.textPaywallSubtitle.setVisibility(View.GONE);
            }
        } else {
            binding.textPaywallSubtitle.setVisibility(View.GONE);
        }
    }

    private void setupStaleState(Feature feature) {
        binding.textLockBadge.setVisibility(View.GONE);
        int titleRes = getContext().getResources().getIdentifier(
                "feature_" + feature.name().toLowerCase(Locale.ROOT),
                "string",
                getContext().getPackageName());
        if (titleRes != 0) {
            binding.textScreenTitle.setText(titleRes);
        }
        binding.textPaywallSubtitle.setVisibility(View.GONE);
        binding.textPaywallIncluded.setVisibility(View.VISIBLE);
        binding.btnCta.setVisibility(View.GONE);
        binding.btnDevSimulate.setVisibility(View.GONE);
        binding.btnContinue.setVisibility(View.VISIBLE);
        binding.btnContinue.setOnClickListener(v -> {
            Bundle res = new Bundle();
            res.putString(NavArgs.RESULT_VALUE, NavArgs.RESULT_PURCHASED);
            getParentFragmentManager().setFragmentResult(NavArgs.RESULT_KEY_PAYWALL, res);
            NavHostFragment.findNavController(this).popBackStack();
        });
        binding.btnNotNow.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
    }

    private void populateComparisonTable() {
        for (int i = 0; i < REPRESENTATIVE_FEATURES.length; i++) {
            Feature f = REPRESENTATIVE_FEATURES[i];
            int titleRes = REPRESENTATIVE_TITLES[i];

            TableRow row = new TableRow(getContext());
            row.setPadding(0, 8, 0, 8);

            TextView textTitle = new TextView(getContext());
            textTitle.setText(titleRes);
            textTitle.setTextAppearance(R.style.TextAppearance_NF_Body);
            textTitle.setTextColor(getContext().getColor(R.color.nf_on_surface));
            TableRow.LayoutParams titleLp = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textTitle.setLayoutParams(titleLp);
            row.addView(textTitle);

            row.addView(createCheckCell(Tier.FREE, f));
            row.addView(createCheckCell(Tier.PRO, f));
            row.addView(createCheckCell(Tier.ENTERPRISE, f));

            binding.tableComparison.addView(row);
        }
    }

    private TextView createCheckCell(Tier tier, Feature feature) {
        TextView cell = new TextView(getContext());
        boolean allowed = EntitlementPolicy.decide(tier, feature).kind() == DecisionKind.ALLOWED;
        cell.setText(allowed ? "✓" : "✕");
        cell.setContentDescription(getString(allowed ? R.string.paywall_yes : R.string.paywall_no));
        cell.setGravity(Gravity.CENTER);
        cell.setTextAppearance(R.style.TextAppearance_NF_Body);
        cell.setPadding(16, 0, 16, 0);
        int colorRes = allowed ? R.color.nf_primary : R.color.nf_on_surface_variant;
        cell.setTextColor(getContext().getColor(colorRes));
        return cell;
    }

    private String tierDisplayName(Tier tier) {
        if (tier == null) return "";
        switch (tier) {
            case PRO:
                return getString(R.string.tier_pro);
            case ENTERPRISE:
                return getString(R.string.tier_enterprise);
            case FREE:
            default:
                return getString(R.string.tier_free);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

