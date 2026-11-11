package com.netfieldpro.data.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.content.Context;

import com.netfieldpro.core.plan.OrphanSweeper;

public class PlanFileStore {

    private final File plansDir;

    public PlanFileStore(Context context) {
        this(new File(context.getFilesDir(), "plans"));
    }

    public PlanFileStore(File plansDir) {
        this.plansDir = plansDir;
        if (!plansDir.exists()) {
            plansDir.mkdirs();
        }
    }

    public File getPlansDir() {
        if (!plansDir.exists()) {
            plansDir.mkdirs();
        }
        return plansDir;
    }

    public File getPlanFile(String floorId, String ext) {
        return new File(getPlansDir(), floorId + "." + ext);
    }

    public File findPlanFile(String floorId) {
        File png = new File(getPlansDir(), floorId + ".png");
        if (png.exists()) {
            return png;
        }
        File jpg = new File(getPlansDir(), floorId + ".jpg");
        if (jpg.exists()) {
            return jpg;
        }
        File jpeg = new File(getPlansDir(), floorId + ".jpeg");
        if (jpeg.exists()) {
            return jpeg;
        }
        return null;
    }

    public File getTempFile(String floorId) {
        return new File(getPlansDir(), floorId + ".tmp");
    }

    public File savePlanBytes(String floorId, String ext, byte[] bytes) throws IOException {
        File tempFile = getTempFile(floorId);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(bytes);
            fos.flush();
        }
        File targetFile = getPlanFile(floorId, ext);
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (!tempFile.renameTo(targetFile)) {
            // Fallback copy if renameTo fails
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(bytes);
            }
            tempFile.delete();
        }
        return targetFile;
    }

    public File savePlanStream(String floorId, String ext, InputStream in) throws IOException {
        File tempFile = getTempFile(floorId);
        try (OutputStream out = new FileOutputStream(tempFile)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
            out.flush();
        }
        File targetFile = getPlanFile(floorId, ext);
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (!tempFile.renameTo(targetFile)) {
            try (InputStream fin = new java.io.FileInputStream(tempFile);
                 OutputStream fos = new FileOutputStream(targetFile)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = fin.read(buf)) != -1) {
                    fos.write(buf, 0, r);
                }
            }
            tempFile.delete();
        }
        return targetFile;
    }

    public boolean deletePlanFile(String floorId) {
        boolean deleted = false;
        File[] candidates = new File[]{
                new File(getPlansDir(), floorId + ".png"),
                new File(getPlansDir(), floorId + ".jpg"),
                new File(getPlansDir(), floorId + ".jpeg"),
                new File(getPlansDir(), floorId + ".tmp")
        };
        for (File f : candidates) {
            if (f.exists() && f.delete()) {
                deleted = true;
            }
        }
        return deleted;
    }

    public List<String> listFileNames() {
        String[] files = getPlansDir().list();
        if (files == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(files);
    }

    public int sweepOrphans(Set<String> existingFloorIds) {
        List<String> fileNames = listFileNames();
        List<String> orphans = OrphanSweeper.findOrphans(fileNames, existingFloorIds);
        int count = 0;
        for (String orphanName : orphans) {
            File f = new File(getPlansDir(), orphanName);
            if (f.exists() && f.delete()) {
                count++;
            }
        }
        return count;
    }
}
