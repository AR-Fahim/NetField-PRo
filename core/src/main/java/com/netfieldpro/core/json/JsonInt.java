package com.netfieldpro.core.json;

import java.util.Objects;

public final class JsonInt implements JsonValue {

    private final long value;

    public JsonInt(long value) {
        this.value = value;
    }

    public long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsonInt jsonInt = (JsonInt) o;
        return value == jsonInt.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "JsonInt(" + value + ")";
    }
}
