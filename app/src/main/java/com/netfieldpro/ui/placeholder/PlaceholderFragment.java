package com.netfieldpro.ui.placeholder;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.permissions.PermAction;
import com.netfieldpro.core.permissions.PermState;
import com.netfieldpro.core.permissions.PermissionKey;
import com.netfieldpro.core.permissions.PermissionPlan;
import com.netfieldpro.core.permissions.PermissionPlanner;
import com.netfieldpro.core.permissions.PermissionSnapshot;
import com.netfieldpro.core.permissions.PrimaryAction;
import com.netfieldpro.databinding.FragmentOnboardingBinding;
import com.netfieldpro.databinding.FragmentPlaceholderBinding;
import com.netfieldpro.onboarding.OnboardingStore;
import com.netfieldpro.onboarding.PermissionProbe;
import com.netfieldpro.ui.NavArgs;

import java.util.List;
import java.util.Map;

public class PlaceholderFragment extends Fragment {

    private FragmentPlaceholderBinding binding;
    private FragmentOnboardingBinding onboardingBinding;

    private String screenId;
    PermissionRequestHook testHook;
    Snackbar lastSnackbar;
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resolveScreenId();
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::onPermissionResult
        );
    }

    private void resolveScreenId() {
        if (getArguments() != null && getArguments().containsKey(NavArgs.SCREEN_ID)) {
            screenId = getArguments().getString(NavArgs.SCREEN_ID);
        }
        if (screenId == null || screenId.isEmpty()) {
            try {
                NavController controller = NavHostFragment.findNavController(this);
                NavDestination currentDest = controller.getCurrentDestination();
                if (currentDest != null) {
                    for (PlaceholderSpec s : PlaceholderSpec.all()) {
                        if (s.getDestinationId() == currentDest.getId()) {
                            screenId = s.getId();
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (screenId == null || screenId.isEmpty()) {
            screenId = "S02";
        }
    }

    public boolean isS01() {
        if (screenId == null) {
            resolveScreenId();
        }
        return "S01".equals(screenId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        resolveScreenId();
        if (isS01()) {
            onboardingBinding = FragmentOnboardingBinding.inflate(inflater, container, false);
            return onboardingBinding.getRoot();
        } else {
            binding = FragmentPlaceholderBinding.inflate(inflater, container, false);
            return binding.getRoot();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (isS01()) {
            setupOnboarding();
        } else {
            setupPlaceholder();
        }
    }

    private void setupOnboarding() {
        onboardingBinding.btnOnboardingPrimary.setOnClickListener(v -> {
            if (getActivity() == null) return;
            PermissionSnapshot snap = NetFieldApp.permissionProbe().snapshot(requireActivity());
            PermissionPlan plan = PermissionPlanner.plan(snap);
            switch (plan.primary()) {
                case CONTINUE -> {
                    NetFieldApp.onboardingStore().markDone();
                    NavOptions options = new NavOptions.Builder().setPopUpTo(R.id.nav_s01, true).build();
                    NavHostFragment.findNavController(this).navigate(R.id.action_s01_to_s02, null, options);
                }
                case ALLOW_LOCATION -> {
                    requestPermissions(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                }
                case OPEN_SETTINGS -> openSettings();
            }
        });

        onboardingBinding.btnPermLocation.setOnClickListener(v -> {
            if (getActivity() == null) return;
            PermissionSnapshot snap = NetFieldApp.permissionProbe().snapshot(requireActivity());
            PermissionPlan plan = PermissionPlanner.plan(snap);
            if (plan.location().action() == PermAction.REQUEST) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            } else if (plan.location().action() == PermAction.OPEN_SETTINGS) {
                openSettings();
            }
        });

        onboardingBinding.btnPermNotif.setOnClickListener(v -> {
            if (getActivity() == null) return;
            PermissionSnapshot snap = NetFieldApp.permissionProbe().snapshot(requireActivity());
            PermissionPlan plan = PermissionPlanner.plan(snap);
            if (plan.notifications().action() == PermAction.REQUEST) {
                requestPermissions(new String[]{
                        Manifest.permission.POST_NOTIFICATIONS
                });
            } else if (plan.notifications().action() == PermAction.OPEN_SETTINGS) {
                openSettings();
            }
        });

        onboardingBinding.btnOnboardingSkip.setOnClickListener(v -> {
            NetFieldApp.onboardingStore().markDone();
            NavOptions options = new NavOptions.Builder().setPopUpTo(R.id.nav_s01, true).build();
            NavHostFragment.findNavController(this).navigate(R.id.action_s01_to_s02, null, options);
        });

        onboardingBinding.btnOnboardingVerify.setOnClickListener(v -> {
            NetFieldApp.onboardingStore().markDone();
            NavHostFragment.findNavController(this).navigate(R.id.action_s01_to_s17);
        });

        render();
    }

    public void render() {
        if (!isS01() || onboardingBinding == null || getActivity() == null) {
            return;
        }

        PermissionProbe probe = NetFieldApp.permissionProbe();
        PermissionSnapshot snapshot = probe.snapshot(getActivity());
        PermissionPlan plan = PermissionPlanner.plan(snapshot);

        // Location state & chip
        String locStateStr = getPermStateString(plan.location().state());
        onboardingBinding.chipPermLocationState.setText(locStateStr);
        setChipColor(onboardingBinding.chipPermLocationState, plan.location().state());

        // Location row button
        if (plan.location().action() == PermAction.NONE) {
            onboardingBinding.btnPermLocation.setVisibility(View.GONE);
        } else {
            onboardingBinding.btnPermLocation.setVisibility(View.VISIBLE);
            if (plan.location().action() == PermAction.REQUEST) {
                onboardingBinding.btnPermLocation.setText(R.string.perm_btn_allow);
                onboardingBinding.btnPermLocation.setContentDescription(
                        getString(R.string.perm_btn_allow_desc, getString(R.string.onboarding_perm_location_title))
                );
            } else {
                onboardingBinding.btnPermLocation.setText(R.string.perm_btn_settings);
                onboardingBinding.btnPermLocation.setContentDescription(
                        getString(R.string.perm_btn_settings_desc, getString(R.string.onboarding_perm_location_title))
                );
            }
        }

        // Location card content description
        onboardingBinding.cardPermLocation.setContentDescription(
                getString(R.string.onboarding_row_desc, getString(R.string.onboarding_perm_location_title), locStateStr)
        );

        // Notifications state & chip
        String notifStateStr = getPermStateString(plan.notifications().state());
        onboardingBinding.chipPermNotifState.setText(notifStateStr);
        setChipColor(onboardingBinding.chipPermNotifState, plan.notifications().state());

        // Notifications row button
        if (plan.notifications().action() == PermAction.NONE) {
            onboardingBinding.btnPermNotif.setVisibility(View.GONE);
        } else {
            onboardingBinding.btnPermNotif.setVisibility(View.VISIBLE);
            if (plan.notifications().action() == PermAction.REQUEST) {
                onboardingBinding.btnPermNotif.setText(R.string.perm_btn_allow);
                onboardingBinding.btnPermNotif.setContentDescription(
                        getString(R.string.perm_btn_allow_desc, getString(R.string.onboarding_perm_notif_title))
                );
            } else {
                onboardingBinding.btnPermNotif.setText(R.string.perm_btn_settings);
                onboardingBinding.btnPermNotif.setContentDescription(
                        getString(R.string.perm_btn_settings_desc, getString(R.string.onboarding_perm_notif_title))
                );
            }
        }

        // Notifications card content description
        onboardingBinding.cardPermNotif.setContentDescription(
                getString(R.string.onboarding_row_desc, getString(R.string.onboarding_perm_notif_title), notifStateStr)
        );

        // Primary button text
        switch (plan.primary()) {
            case CONTINUE -> onboardingBinding.btnOnboardingPrimary.setText(R.string.onboarding_primary_continue);
            case ALLOW_LOCATION -> onboardingBinding.btnOnboardingPrimary.setText(R.string.onboarding_primary_allow);
            case OPEN_SETTINGS -> onboardingBinding.btnOnboardingPrimary.setText(R.string.onboarding_primary_settings);
        }
    }

    private String getPermStateString(PermState state) {
        return switch (state) {
            case GRANTED -> getString(R.string.perm_state_granted);
            case DENIED_CAN_ASK -> getString(R.string.perm_state_can_ask);
            case DENIED_PERMANENT -> getString(R.string.perm_state_permanent);
            case APPROXIMATE_ONLY -> getString(R.string.perm_state_approx);
            case NOT_REQUIRED -> getString(R.string.perm_state_not_required);
        };
    }

    private void setChipColor(Chip chip, PermState state) {
        int colorRes = switch (state) {
            case GRANTED -> R.color.nf_primary;
            case DENIED_PERMANENT, APPROXIMATE_ONLY -> R.color.nf_warning;
            case DENIED_CAN_ASK, NOT_REQUIRED -> R.color.nf_on_surface_variant;
        };
        int color = ContextCompat.getColor(requireContext(), colorRes);
        chip.setTextColor(color);
        chip.setChipStrokeColor(ColorStateList.valueOf(color));
    }

    public void onPermissionResult(Map<String, Boolean> r) {
        if (r == null) return;
        OnboardingStore store = NetFieldApp.onboardingStore();
        if (r.containsKey(Manifest.permission.ACCESS_FINE_LOCATION)
                || r.containsKey(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            store.markAsked(PermissionKey.LOCATION);
        }
        if (r.containsKey(Manifest.permission.POST_NOTIFICATIONS)) {
            store.markAsked(PermissionKey.NOTIFICATIONS);
        }
        render();
    }

    public void requestPermissions(String[] permissions) {
        if (testHook != null) {
            testHook.onRequest(permissions);
        } else if (permissionLauncher != null) {
            permissionLauncher.launch(permissions);
        }
    }

    public void openSettings() {
        Activity a = getActivity();
        if (a == null) return;
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", a.getPackageName(), null));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            View root = getView();
            if (root != null) {
                lastSnackbar = Snackbar.make(root, R.string.onboarding_settings_error, Snackbar.LENGTH_SHORT);
                lastSnackbar.show();
            }
        }
    }

    public void setTestHook(PermissionRequestHook hook) {
        this.testHook = hook;
    }

    public Snackbar getLastSnackbar() {
        return lastSnackbar;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isS01()) {
            render();
        }
    }

    private void setupPlaceholder() {
        PlaceholderSpec spec = PlaceholderSpec.get(screenId);
        if (spec == null) {
            binding.textScreenId.setText(screenId);
            binding.textScreenTitle.setText(screenId);
            binding.cardNextScreens.setVisibility(View.GONE);
            return;
        }

        binding.textScreenId.setText(spec.getId());
        binding.textScreenTitle.setText(spec.getTitleRes());
        binding.textPlaceholderArrives.setText(getString(R.string.placeholder_arrives_format, spec.getOwner()));
        binding.textOwner.setText(getString(R.string.placeholder_owner_format, spec.getOwner()));

        List<String> outgoing = spec.getOutgoingScreenIds();
        if (outgoing.isEmpty()) {
            binding.cardNextScreens.setVisibility(View.GONE);
        } else {
            binding.cardNextScreens.setVisibility(View.VISIBLE);
            binding.layoutNextButtons.removeAllViews();
            for (String targetId : outgoing) {
                PlaceholderSpec targetSpec = PlaceholderSpec.get(targetId);
                if (targetSpec == null) {
                    continue;
                }
                MaterialButton btn = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        getResources().getDimensionPixelSize(R.dimen.touch_primary)
                );
                lp.topMargin = getResources().getDimensionPixelSize(R.dimen.space_1);
                btn.setLayoutParams(lp);
                String label = getString(R.string.edge_button_format, targetSpec.getId(), getString(targetSpec.getTitleRes()));
                btn.setText(label);
                btn.setOnClickListener(v -> {
                    NavHostFragment.findNavController(PlaceholderFragment.this).navigate(targetSpec.getDestinationId());
                });
                binding.layoutNextButtons.addView(btn);
            }
        }

        setupExtensionsIfS19(screenId);
    }

    protected void setupExtensionsIfS19(String screenId) {
        if ("S19".equals(screenId)) {
            List<com.netfieldpro.ui.settings.SettingsRow> allRows =
                    com.netfieldpro.ui.settings.SettingsExtensionRegistry.getInstance().rows();
            List<com.netfieldpro.ui.settings.SettingsRow> visibleRows = new java.util.ArrayList<>();
            for (com.netfieldpro.ui.settings.SettingsRow r : allRows) {
                if (r.isVisible()) {
                    visibleRows.add(r);
                }
            }

            if (visibleRows.isEmpty()) {
                binding.layoutExtensions.setVisibility(View.GONE);
            } else {
                binding.layoutExtensions.setVisibility(View.VISIBLE);
                binding.layoutExtensionRows.removeAllViews();
                for (com.netfieldpro.ui.settings.SettingsRow row : visibleRows) {
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
        } else {
            binding.layoutExtensions.setVisibility(View.GONE);
        }
    }

    public FragmentPlaceholderBinding getBinding() {
        return binding;
    }

    public FragmentOnboardingBinding getOnboardingBinding() {
        return onboardingBinding;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        onboardingBinding = null;
    }
}
