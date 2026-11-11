package com.netfieldpro.core.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class JsonArray implements JsonValue {

    private final List<JsonValue> elements = new ArrayList<>();

    public JsonArray() {
    }

    public void add(JsonValue value) {
        if (value == null) {
            throw new IllegalArgumentException("Array element cannot be null");
        }
        elements.add(value);
    }

    public JsonValue get(int index) {
        return elements.get(index);
    }

    public int size() {
        return elements.size();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public List<JsonValue> elements() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsonArray jsonArray = (JsonArray) o;
        return Objects.equals(elements, jsonArray.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements);
    }

    @Override
    public String toString() {
        return "JsonArray" + elements;
    }
}
