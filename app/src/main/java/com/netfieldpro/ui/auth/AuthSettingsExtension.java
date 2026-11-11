package com.netfieldpro.ui.auth;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.netfieldpro.R;
import com.netfieldpro.auth.AuthGatewayFactory;
import com.netfieldpro.auth.AuthSessionGateway;
import com.netfieldpro.core.auth.SignOutDecision;
import com.netfieldpro.core.auth.SignOutPolicy;
import com.netfieldpro.session.SessionRuntimeStore;
import com.netfieldpro.ui.settings.SettingsExtensionRegistry;
import com.netfieldpro.ui.settings.SettingsRow;

public final class AuthSettingsExtension {

    public static final String ROW_ID = "row_logout_alpha";

    private AuthSettingsExtension() {}

    public static void setup(@NonNull Fragment fragment) {
        setup(fragment, AuthGatewayFactory.create(), SessionRuntimeStore.create(fragment.requireContext()));
    }

    public static void setup(@NonNull Fragment fragment, AuthSessionGateway gateway, SessionRuntimeStore store) {
        boolean supports = gateway != null && gateway.supportsAccounts();
        String email = (gateway != null) ? gateway.currentEmail() : null;

        SettingsRow row = new SettingsRow(
                ROW_ID,
                "Log Out (Alpha)",
                email,
                () -> handleLogoutClick(fragment, gateway, store),
                supports
        );
        SettingsExtensionRegistry.getInstance().registerOrReplace(row);
    }

    public static void handleLogoutClick(@NonNull Fragment fragment, AuthSessionGateway gateway, SessionRuntimeStore store) {
        boolean anyActive = store != null && store.anyActive();
        SignOutDecision decision = SignOutPolicy.decide(anyActive);

        if (decision == SignOutDecision.BLOCKED_ACTIVE_SURVEY) {
            if (fragment.getView() != null) {
                Snackbar.make(fragment.requireView(), R.string.auth_signout_blocked, Snackbar.LENGTH_SHORT).show();
            }
            return;
        }

        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.auth_logout_title)
                .setMessage(R.string.auth_logout_body)
                .setPositiveButton(R.string.auth_logout_confirm, (dialog, which) -> {
                    if (gateway != null) {
                        gateway.signOut();
                    }
                    NavController navController = NavHostFragment.findNavController(fragment);
                    NavOptions navOptions = new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_s02, true)
                            .build();
                    navController.navigate(R.id.nav_auth_slot, null, navOptions);
                })
                .setNegativeButton(R.string.auth_logout_cancel, null)
                .show();
    }
}

