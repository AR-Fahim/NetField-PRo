package com.netfieldpro.core.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class OrphanSweeper {

    private static final Pattern ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{22}$");

    private OrphanSweeper() {
    }

    public static boolean isOrphan(String fileName, Set<String> existingFloorIds) {
        if (fileName == null) {
            return false;
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0) {
            return false;
        }
        String baseName = fileName.substring(0, lastDot);
        String ext = fileName.substring(lastDot).toLowerCase();

        if (!ID_PATTERN.matcher(baseName).matches()) {
            return false;
        }

        if (".tmp".equals(ext)) {
            return true;
        }

        if (".png".equals(ext) || ".jpg".equals(ext) || ".jpeg".equals(ext)) {
            return existingFloorIds == null || !existingFloorIds.contains(baseName);
        }

        return false;
    }

    public static List<String> findOrphans(Collection<String> fileNames, Set<String> existingFloorIds) {
        if (fileNames == null || fileNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> orphans = new ArrayList<>();
        for (String fileName : fileNames) {
            if (isOrphan(fileName, existingFloorIds)) {
                orphans.add(fileName);
            }
        }
        return orphans;
    }
}
