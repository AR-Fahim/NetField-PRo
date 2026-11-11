package com.netfieldpro.ui.auth;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.color.MaterialColors;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.nav.StartRouter;
import com.netfieldpro.databinding.FragmentAuthGateBinding;

public class AuthGateFragment extends Fragment {

    private FragmentAuthGateBinding binding;
    private AuthGateViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAuthGateBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthGateViewModel.class);

        binding.btnSignIn.setOnClickListener(v -> handleSignIn());
        binding.btnCreateAccount.setOnClickListener(v -> handleSignUp());
        binding.btnForgotPassword.setOnClickListener(v -> handleForgotPassword());
        binding.btnVerifyOnly.setOnClickListener(v -> handleVerifyOnly());

        binding.editPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleSignIn();
                return true;
            }
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
    }

    private void handleSignIn() {
        if (viewModel.isLoading()) {
            return;
        }
        String email = binding.editEmail.getText() != null ? binding.editEmail.getText().toString() : "";
        String password = binding.editPassword.getText() != null ? binding.editPassword.getText().toString() : "";
        viewModel.signIn(email, password);
    }

    private void handleSignUp() {
        if (viewModel.isLoading()) {
            return;
        }
        String email = binding.editEmail.getText() != null ? binding.editEmail.getText().toString() : "";
        String password = binding.editPassword.getText() != null ? binding.editPassword.getText().toString() : "";
        viewModel.signUp(email, password);
    }

    private void handleForgotPassword() {
        if (viewModel.isLoading()) {
            return;
        }
        String email = binding.editEmail.getText() != null ? binding.editEmail.getText().toString() : "";
        viewModel.resetPassword(email);
    }

    private void handleVerifyOnly() {
        if (viewModel.isLoading()) {
            return;
        }
        NavHostFragment.findNavController(this).navigate(R.id.nav_s17);
    }

    private void renderState(AuthUiState state) {
        if (state == null) {
            return;
        }
        boolean loading = state.isLoading();
        binding.progressAuth.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSignIn.setEnabled(!loading);
        binding.btnCreateAccount.setEnabled(!loading);
        binding.btnForgotPassword.setEnabled(!loading);
        binding.btnVerifyOnly.setEnabled(!loading);

        if (state.getStatus() == AuthUiState.Status.SUCCESS) {
            boolean firstLaunchDone = NetFieldApp.onboardingStore().isDone();
            StartRouter.StartDestination dest = StartRouter.route(
                    StartRouter.StartIntent.MAIN,
                    true,
                    firstLaunchDone
            );
            int targetDestId = (dest == StartRouter.StartDestination.S01) ? R.id.nav_s01 : R.id.nav_s02;
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_auth_slot, true)
                    .build();
            NavHostFragment.findNavController(this).navigate(targetDestId, null, navOptions);
            return;
        }

        if (state.getMessageResId() != 0) {
            binding.textAuthStatus.setText(state.getMessageResId());
        } else {
            binding.textAuthStatus.setText("");
        }

        Context ctx = getContext();
        if (ctx != null) {
            if (state.isError()) {
                int errorColor = MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorError);
                binding.textAuthStatus.setTextColor(errorColor);
            } else if (state.getStatus() == AuthUiState.Status.INFO) {
                int primaryColor = MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary);
                binding.textAuthStatus.setTextColor(primaryColor);
            } else {
                int onSurfaceColor = MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorOnSurface);
                binding.textAuthStatus.setTextColor(onSurfaceColor);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

