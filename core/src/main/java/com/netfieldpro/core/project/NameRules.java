package com.netfieldpro.core.project;

public final class NameRules {

    public enum Status {
        OK,
        EMPTY,
        TOO_LONG,
        INVALID_CHARS
    }

    private NameRules() {
    }

    public static Status validate(String name) {
        if (name == null) {
            return Status.EMPTY;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return Status.EMPTY;
        }
        int codePoints = trimmed.codePointCount(0, trimmed.length());
        if (codePoints > 60) {
            return Status.TOO_LONG;
        }
        for (int i = 0; i < trimmed.length(); ) {
            int cp = trimmed.codePointAt(i);
            if (cp < 0x20) {
                return Status.INVALID_CHARS;
            }
            i += Character.charCount(cp);
        }
        return Status.OK;
    }

    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.trim();
    }

    public static String normalizeSite(String site) {
        if (site == null) {
            return null;
        }
        String trimmed = site.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static Status validateSite(String site) {
        if (site == null) {
            return Status.OK;
        }
        String trimmed = site.trim();
        if (trimmed.isEmpty()) {
            return Status.OK;
        }
        int codePoints = trimmed.codePointCount(0, trimmed.length());
        if (codePoints > 60) {
            return Status.TOO_LONG;
        }
        for (int i = 0; i < trimmed.length(); ) {
            int cp = trimmed.codePointAt(i);
            if (cp < 0x20) {
                return Status.INVALID_CHARS;
            }
            i += Character.charCount(cp);
        }
        return Status.OK;
    }
}
