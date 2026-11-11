package com.netfieldpro.core.heat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class Imputer {

    public static final int IMPUTED_TENTHS = -950;

    public record Entry(String bssid, int tenths, boolean imputed) {
    }

    private Imputer() {
    }

    public static List<Entry> resolve(Map<String, Integer> measuredTenthsByBssid, List<String> universe) {
        if (universe == null || universe.isEmpty()) {
            return Collections.emptyList();
        }
        List<Entry> result = new ArrayList<>(universe.size());
        for (String bssid : universe) {
            if (measuredTenthsByBssid != null && measuredTenthsByBssid.containsKey(bssid)) {
                Integer val = measuredTenthsByBssid.get(bssid);
                if (val != null) {
                    result.add(new Entry(bssid, val, false));
                    continue;
                }
            }
            result.add(new Entry(bssid, IMPUTED_TENTHS, true));
        }
        return Collections.unmodifiableList(result);
    }
}
