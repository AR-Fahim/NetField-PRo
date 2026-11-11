package com.netfieldpro.core.json;

import java.util.Objects;

public final class JsonString implements JsonValue {

    private final String value;

    public JsonString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("String value cannot be null");
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsonString that = (JsonString) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "JsonString(" + value + ")";
    }
}
