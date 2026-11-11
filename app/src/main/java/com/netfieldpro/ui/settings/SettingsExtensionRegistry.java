package com.netfieldpro.ui.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SettingsExtensionRegistry {

    private static final SettingsExtensionRegistry INSTANCE = new SettingsExtensionRegistry();

    public static SettingsExtensionRegistry getInstance() {
        return INSTANCE;
    }

    private final Map<String, SettingsRow> registry = new LinkedHashMap<>();

    private SettingsExtensionRegistry() {}

    public synchronized void register(SettingsRow row) {
        if (row == null || row.getId() == null) {
            throw new IllegalArgumentException("Row or ID cannot be null");
        }
        if (registry.containsKey(row.getId())) {
            throw new IllegalArgumentException("Duplicate settings row id: " + row.getId());
        }
        registry.put(row.getId(), row);
    }

    public synchronized void registerOrReplace(SettingsRow row) {
        if (row == null || row.getId() == null) {
            throw new IllegalArgumentException("Row or ID cannot be null");
        }
        registry.put(row.getId(), row);
    }

    public synchronized void unregister(String id) {
        if (id != null) {
            registry.remove(id);
        }
    }

    public synchronized List<SettingsRow> rows() {
        return Collections.unmodifiableList(new ArrayList<>(registry.values()));
    }

    public synchronized void clear() {
        registry.clear();
    }
}

