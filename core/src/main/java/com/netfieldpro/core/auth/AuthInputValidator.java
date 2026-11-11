package com.netfieldpro.core.auth;

import java.util.regex.Pattern;

public final class AuthInputValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private AuthInputValidator() {
    }

    public static String normalizeEmail(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }

    public static InputCheck checkEmail(String email) {
        String trimmed = normalizeEmail(email);
        if (trimmed.isEmpty()) {
            return InputCheck.EMAIL_EMPTY;
        }
        if (trimmed.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(trimmed).matches()) {
            return InputCheck.EMAIL_INVALID;
        }
        return InputCheck.OK;
    }

    public static InputCheck signIn(String email, String pw) {
        InputCheck emailCheck = checkEmail(email);
        if (emailCheck != InputCheck.OK) {
            return emailCheck;
        }
        if (pw == null || pw.isEmpty()) {
            return InputCheck.PASSWORD_EMPTY;
        }
        return InputCheck.OK;
    }

    public static InputCheck signUp(String email, String pw) {
        InputCheck emailCheck = checkEmail(email);
        if (emailCheck != InputCheck.OK) {
            return emailCheck;
        }
        if (pw == null || pw.length() < MIN_PASSWORD_LENGTH) {
            return InputCheck.PASSWORD_TOO_SHORT;
        }
        if (pw.length() > MAX_PASSWORD_LENGTH) {
            return InputCheck.PASSWORD_TOO_LONG;
        }
        return InputCheck.OK;
    }

    public static InputCheck reset(String email) {
        return checkEmail(email);
    }
}

