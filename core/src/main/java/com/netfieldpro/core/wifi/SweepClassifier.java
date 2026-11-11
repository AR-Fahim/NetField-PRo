package com.netfieldpro.core.wifi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.netfieldpro.core.model.enums.SweepClass;

public final class SweepClassifier {
    private SweepClassifier() {}

    public static List<Integer> referenceSet(List<Set<Integer>> scans) {
        if (scans == null || scans.size() != 3) {
            throw new IllegalArgumentException("scans must have exactly 3 sets");
        }
        Map<Integer, Integer> counts = new HashMap<>();
        for (Set<Integer> scan : scans) {
            for (Integer freq : scan) {
                counts.put(freq, counts.getOrDefault(freq, 0) + 1);
            }
        }
        List<Integer> ref = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() >= 2) {
                ref.add(entry.getKey());
            }
        }
        Collections.sort(ref);
        return Collections.unmodifiableList(ref);
    }

    public static SweepClass classify(Set<Integer> ref, Set<Integer> sweepFreqs) {
        if (ref == null || ref.isEmpty()) {
            throw new IllegalArgumentException("ref cannot be null or empty");
        }
        if (sweepFreqs == null) {
            throw new IllegalArgumentException("sweepFreqs cannot be null");
        }
        int intersectionSize = 0;
        for (Integer f : sweepFreqs) {
            if (ref.contains(f)) {
                intersectionSize++;
            }
        }
        if (5L * intersectionSize >= 3L * ref.size()) {
            return SweepClass.FULL;
        } else {
            return SweepClass.PARTIAL;
        }
    }

    public static SweepClass classify(List<Integer> ref, Set<Integer> sweepFreqs) {
        if (ref == null || ref.isEmpty()) {
            throw new IllegalArgumentException("ref cannot be null or empty");
        }
        return classify(new HashSet<>(ref), sweepFreqs);
    }
}
