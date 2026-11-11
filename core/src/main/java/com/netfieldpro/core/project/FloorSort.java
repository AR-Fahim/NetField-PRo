package com.netfieldpro.core.project;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.netfieldpro.core.model.entity.Floor;

public final class FloorSort {

    public static final Comparator<Floor> COMPARATOR = (f1, f2) -> {
        int c = Long.compare(f1.levelIndex(), f2.levelIndex());
        if (c != 0) {
            return c;
        }
        return f1.id().compareTo(f2.id());
    };

    private FloorSort() {
    }

    public static List<Floor> sort(List<Floor> floors) {
        if (floors == null) {
            return new ArrayList<>();
        }
        List<Floor> copy = new ArrayList<>(floors);
        copy.sort(COMPARATOR);
        return copy;
    }
}
