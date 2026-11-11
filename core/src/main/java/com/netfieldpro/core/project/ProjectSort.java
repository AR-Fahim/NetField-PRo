package com.netfieldpro.core.project;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.netfieldpro.core.model.entity.Project;

public final class ProjectSort {

    public static final Comparator<Project> COMPARATOR = (p1, p2) -> {
        int c = Long.compare(p2.createdAtMs(), p1.createdAtMs());
        if (c != 0) {
            return c;
        }
        return p1.id().compareTo(p2.id());
    };

    private ProjectSort() {
    }

    public static List<Project> sort(List<Project> projects) {
        if (projects == null) {
            return new ArrayList<>();
        }
        List<Project> copy = new ArrayList<>(projects);
        copy.sort(COMPARATOR);
        return copy;
    }
}
