package com.netfieldpro.ui.placeholder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.netfieldpro.R;

public class PlaceholderSpec {

    private final String id;
    private final int titleRes;
    private final String owner;
    private final int destinationId;
    private final List<String> outgoingScreenIds;

    public PlaceholderSpec(String id, int titleRes, String owner, int destinationId, List<String> outgoingScreenIds) {
        this.id = id;
        this.titleRes = titleRes;
        this.owner = owner;
        this.destinationId = destinationId;
        this.outgoingScreenIds = Collections.unmodifiableList(new ArrayList<>(outgoingScreenIds));
    }

    public String getId() {
        return id;
    }

    public int getTitleRes() {
        return titleRes;
    }

    public String getOwner() {
        return owner;
    }

    public int getDestinationId() {
        return destinationId;
    }

    public List<String> getOutgoingScreenIds() {
        return outgoingScreenIds;
    }

    private static final Map<String, PlaceholderSpec> REGISTRY = new LinkedHashMap<>();

    private static void register(String id, int titleRes, String owner, int destinationId, String... outgoing) {
        REGISTRY.put(id, new PlaceholderSpec(id, titleRes, owner, destinationId, Arrays.asList(outgoing)));
    }

    static {
        register("S01", R.string.screen_s01_title, "M3", R.id.nav_s01, "S02", "S17");
        register("S02", R.string.screen_s02_title, "M6", R.id.nav_s02, "S03", "S17", "S18", "S19");
        register("S03", R.string.screen_s03_title, "M6", R.id.nav_s03, "S04", "S06", "S09", "S14");
        register("S04", R.string.screen_s04_title, "M6", R.id.nav_s04, "S05");
        register("S05", R.string.screen_s05_title, "M6", R.id.nav_s05);
        register("S06", R.string.screen_s06_title, "M7s", R.id.nav_s06, "S07", "S18");
        register("S07", R.string.screen_s07_title, "M8a/M7b", R.id.nav_s07, "S08", "S09");
        register("S08", R.string.screen_s08_title, "M8a", R.id.nav_s08);
        register("S09", R.string.screen_s09_title, "M10", R.id.nav_s09, "S10", "S11", "S12", "S13", "S18", "S21");
        register("S10", R.string.screen_s10_title, "M10b", R.id.nav_s10);
        register("S11", R.string.screen_s11_title, "M10c", R.id.nav_s11);
        register("S12", R.string.screen_s12_title, "M16", R.id.nav_s12);
        register("S13", R.string.screen_s13_title, "M11", R.id.nav_s13, "S15", "S18");
        register("S14", R.string.screen_s14_title, "M11", R.id.nav_s14, "S15");
        register("S15", R.string.screen_s15_title, "M11", R.id.nav_s15, "S16");
        register("S16", R.string.screen_s16_title, "M12", R.id.nav_s16, "S18");
        register("S17", R.string.screen_s17_title, "M11", R.id.nav_s17);
        register("S18", R.string.screen_s18_title, "M13", R.id.nav_s18);
        register("S19", R.string.screen_s19_title, "M2", R.id.nav_s19, "S18", "S20", "S21");
        register("S20", R.string.screen_s20_title, "M4/M5/M11", R.id.nav_s20);
        register("S21", R.string.screen_s21_title, "M11a", R.id.nav_s21);
    }

    public static PlaceholderSpec get(String id) {
        return REGISTRY.get(id);
    }

    public static List<PlaceholderSpec> all() {
        return new ArrayList<>(REGISTRY.values());
    }
}

