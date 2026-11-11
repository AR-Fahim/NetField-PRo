package com.netfieldpro.report;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public final class ReportFileManager {

    public static final long ONE_DAY_MS = 24L * 60L * 60L * 1000L;

    private ReportFileManager() {
    }

    public static String sanitizeAscii(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Report";
        }
        String sanitized = input.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
        sanitized = sanitized.replaceAll("_+", "_");
        sanitized = sanitized.replaceAll("^_+|_+$", "");
        return sanitized.isEmpty() ? "Report" : sanitized;
    }

    public static String generateFileName(@Nullable String site, @Nullable String project, long timestampMs) {
        String base = (site != null && !site.trim().isEmpty()) ? site : ((project != null && !project.trim().isEmpty()) ? project : "Audit");
        String sanitized = sanitizeAscii(base);

        long time = (timestampMs > 0) ? timestampMs : 0L;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
        String dateStr = sdf.format(time);

        return String.format(Locale.US, "NetField-%s-%s.pdf", sanitized, dateStr);
    }

    public static File getReportsDir(@NonNull Context context) {
        File dir = new File(context.getCacheDir(), "reports");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static int cleanOldReports(@NonNull Context context, long nowMs) {
        File dir = new File(context.getCacheDir(), "reports");
        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return 0;
        }
        int deleted = 0;
        for (File f : files) {
            if (f.isFile() && (nowMs - f.lastModified() > ONE_DAY_MS)) {
                if (f.delete()) {
                    deleted++;
                }
            }
        }
        return deleted;
    }

    public static Uri getFileUri(@NonNull Context context, @NonNull File file) {
        try {
            return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        } catch (IllegalArgumentException e) {
            // AndroidX FileProvider in core:1.13.0 has a hardcoded '/' separator in belongsToRoot,
            // which fails on Windows host JVM during Robolectric unit tests.
            return Uri.parse("content://" + context.getPackageName() + ".fileprovider/reports/" + file.getName());
        }
    }

    public static Intent createShareIntent(@NonNull Context context, @NonNull File file) {
        Uri uri = getFileUri(context, file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(intent, "Share PDF");
    }

    public static Intent createViewIntent(@NonNull Context context, @NonNull File file) {
        Uri uri = getFileUri(context, file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }
}

