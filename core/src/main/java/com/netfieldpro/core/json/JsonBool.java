package com.netfieldpro.core.json;

import java.util.Objects;

public final class JsonBool implements JsonValue {

    public static final JsonBool TRUE = new JsonBool(true);
    public static final JsonBool FALSE = new JsonBool(false);

    private final boolean value;

    public JsonBool(boolean value) {
        this.value = value;
    }

    public static JsonBool of(boolean value) {
        return value ? TRUE : FALSE;
    }

    public boolean value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsonBool jsonBool = (JsonBool) o;
        return value == jsonBool.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "JsonBool(" + value + ")";
    }
}
