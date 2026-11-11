package com.netfieldpro.ui.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.netfieldpro.R;
import com.netfieldpro.auth.AuthGatewayFactory;
import com.netfieldpro.auth.AuthSessionGateway;
import com.netfieldpro.core.auth.AuthInputValidator;
import com.netfieldpro.core.auth.AuthOutcome;
import com.netfieldpro.core.auth.InputCheck;

public class AuthGateViewModel extends ViewModel {

    private final AuthSessionGateway gateway;
    private final MutableLiveData<AuthUiState> uiState = new MutableLiveData<>(AuthUiState.idle());

    public AuthGateViewModel() {
        this(AuthGatewayFactory.create());
    }

    public AuthGateViewModel(AuthSessionGateway gateway) {
        this.gateway = gateway != null ? gateway : AuthGatewayFactory.create();
    }

    public LiveData<AuthUiState> getUiState() {
        return uiState;
    }

    public boolean isLoading() {
        AuthUiState current = uiState.getValue();
        return current != null && current.isLoading();
    }

    public void signIn(String rawEmail, String rawPassword) {
        if (isLoading()) {
            return;
        }
        InputCheck check = AuthInputValidator.signIn(rawEmail, rawPassword);
        if (check != InputCheck.OK) {
            uiState.setValue(AuthUiState.error(mapInputCheck(check)));
            return;
        }
        uiState.setValue(AuthUiState.loading(R.string.auth_signing_in));
        String email = AuthInputValidator.normalizeEmail(rawEmail);
        gateway.signIn(email, rawPassword, outcome -> {
            if (outcome == AuthOutcome.OK) {
                uiState.setValue(AuthUiState.success());
            } else {
                uiState.setValue(AuthUiState.error(mapOutcome(outcome)));
            }
        });
    }

    public void signUp(String rawEmail, String rawPassword) {
        if (isLoading()) {
            return;
        }
        InputCheck check = AuthInputValidator.signUp(rawEmail, rawPassword);
        if (check != InputCheck.OK) {
            uiState.setValue(AuthUiState.error(mapInputCheck(check)));
            return;
        }
        uiState.setValue(AuthUiState.loading(R.string.auth_creating));
        String email = AuthInputValidator.normalizeEmail(rawEmail);
        gateway.signUp(email, rawPassword, outcome -> {
            if (outcome == AuthOutcome.OK) {
                uiState.setValue(AuthUiState.success());
            } else {
                uiState.setValue(AuthUiState.error(mapOutcome(outcome)));
            }
        });
    }

    public void resetPassword(String rawEmail) {
        if (isLoading()) {
            return;
        }
        InputCheck check = AuthInputValidator.reset(rawEmail);
        if (check != InputCheck.OK) {
            uiState.setValue(AuthUiState.error(mapInputCheck(check)));
            return;
        }
        uiState.setValue(AuthUiState.loading(0));
        String email = AuthInputValidator.normalizeEmail(rawEmail);
        gateway.sendPasswordReset(email, outcome -> {
            if (outcome == AuthOutcome.OK) {
                uiState.setValue(AuthUiState.info(R.string.auth_reset_sent));
            } else {
                uiState.setValue(AuthUiState.error(mapOutcome(outcome)));
            }
        });
    }

    private static int mapInputCheck(InputCheck check) {
        return switch (check) {
            case EMAIL_EMPTY -> R.string.auth_in_email_empty;
            case EMAIL_INVALID -> R.string.auth_in_email_invalid;
            case PASSWORD_EMPTY -> R.string.auth_in_pw_empty;
            case PASSWORD_TOO_SHORT -> R.string.auth_in_pw_short;
            case PASSWORD_TOO_LONG -> R.string.auth_in_pw_long;
            case OK -> 0;
        };
    }

    private static int mapOutcome(AuthOutcome outcome) {
        return switch (outcome) {
            case OK -> 0;
            case INVALID_CREDENTIALS -> R.string.auth_err_invalid_credentials;
            case EMAIL_IN_USE -> R.string.auth_err_email_in_use;
            case WEAK_PASSWORD -> R.string.auth_err_weak_password;
            case INVALID_EMAIL -> R.string.auth_err_invalid_email;
            case NO_NETWORK -> R.string.auth_err_no_network;
            case TOO_MANY_REQUESTS -> R.string.auth_err_too_many;
            case PROVIDER_DISABLED -> R.string.auth_err_provider;
            case UNKNOWN -> R.string.auth_err_unknown;
        };
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final AuthSessionGateway gateway;

        public Factory(AuthSessionGateway gateway) {
            this.gateway = gateway;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new AuthGateViewModel(gateway);
        }
    }
}

