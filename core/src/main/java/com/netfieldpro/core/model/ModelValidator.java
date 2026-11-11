package com.netfieldpro.core.model;

import java.util.List;

import com.netfieldpro.core.util.IdGenerator;

public final class ModelValidator {

    private ModelValidator() {
    }

    public static void checkRequired(String entity, String field, Object val) {
        if (val == null) {
            throw new IllegalArgumentException(entity + "." + field + ": required field is null");
        }
    }

    public static void checkId(String entity, String field, String id) {
        checkRequired(entity, field, id);
        if (!IdGenerator.isValid(id)) {
            throw new IllegalArgumentException(entity + "." + field + ": invalid id format: " + id);
        }
    }

    public static void checkOptionalId(String entity, String field, String id) {
        if (id != null && !IdGenerator.isValid(id)) {
            throw new IllegalArgumentException(entity + "." + field + ": invalid id format: " + id);
        }
    }

    public static void checkString(String entity, String field, String s, int maxLen) {
        if (s != null && s.length() > maxLen) {
            throw new IllegalArgumentException(entity + "." + field + ": length exceeds " + maxLen);
        }
    }

    public static void checkDouble(String entity, String field, double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            throw new IllegalArgumentException(entity + "." + field + ": double must be finite: " + v);
        }
    }

    public static void checkOptionalDouble(String entity, String field, Double v) {
        if (v != null && (Double.isNaN(v) || Double.isInfinite(v))) {
            throw new IllegalArgumentException(entity + "." + field + ": double must be finite: " + v);
        }
    }

    public static void checkNonNegative(String entity, String field, long count) {
        if (count < 0) {
            throw new IllegalArgumentException(entity + "." + field + ": must be >= 0: " + count);
        }
    }

    public static void checkPositive(String entity, String field, long count) {
        if (count <= 0) {
            throw new IllegalArgumentException(entity + "." + field + ": must be > 0: " + count);
        }
    }

    public static void checkPositive(String entity, String field, double val) {
        checkDouble(entity, field, val);
        if (val <= 0.0) {
            throw new IllegalArgumentException(entity + "." + field + ": must be > 0: " + val);
        }
    }

    public static <T> List<T> checkList(String entity, String field, List<T> list) {
        checkRequired(entity, field, list);
        for (T item : list) {
            if (item == null) {
                throw new IllegalArgumentException(entity + "." + field + ": list element cannot be null");
            }
        }
        return List.copyOf(list);
    }

    public static <T> List<T> checkOptionalList(String entity, String field, List<T> list) {
        if (list == null) {
            return null;
        }
        return checkList(entity, field, list);
    }
}

