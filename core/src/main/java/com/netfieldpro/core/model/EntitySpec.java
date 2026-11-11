package com.netfieldpro.core.model;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Supplier;

import com.netfieldpro.core.json.JsonObject;

public record EntitySpec<T>(
        String name,
        Class<T> entityClass,
        Supplier<T> sampleFactory,
        Function<T, JsonObject> toJson,
        Comparator<T> sortComparator
) {
}

