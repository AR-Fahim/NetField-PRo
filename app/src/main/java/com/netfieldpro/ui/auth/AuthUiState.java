package com.netfieldpro.ui.auth;

public class AuthUiState {

    public enum Status {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR,
        INFO
    }

    private final Status status;
    private final int messageResId;
    private final boolean isError;

    public AuthUiState(Status status, int messageResId, boolean isError) {
        this.status = status;
        this.messageResId = messageResId;
        this.isError = isError;
    }

    public static AuthUiState idle() {
        return new AuthUiState(Status.IDLE, 0, false);
    }

    public static AuthUiState loading(int messageResId) {
        return new AuthUiState(Status.LOADING, messageResId, false);
    }

    public static AuthUiState success() {
        return new AuthUiState(Status.SUCCESS, 0, false);
    }

    public static AuthUiState error(int messageResId) {
        return new AuthUiState(Status.ERROR, messageResId, true);
    }

    public static AuthUiState info(int messageResId) {
        return new AuthUiState(Status.INFO, messageResId, false);
    }

    public Status getStatus() {
        return status;
    }

    public int getMessageResId() {
        return messageResId;
    }

    public boolean isError() {
        return isError;
    }

    public boolean isLoading() {
        return status == Status.LOADING;
    }
}

