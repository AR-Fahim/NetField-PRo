package com.netfieldpro.data.plan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.media.ExifInterface;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.netfieldpro.R;
import com.netfieldpro.core.plan.PlanImportValidator;
import com.netfieldpro.core.plan.PlanStoragePolicy;

public final class PlanImporter {

    public record ImportResult(
            boolean success,
            @StringRes int errorStringRes,
            @Nullable File storedFile,
            @Nullable String sha256,
            int width,
            int height,
            @Nullable String extension
    ) {
        public static ImportResult error(@StringRes int errorRes) {
            return new ImportResult(false, errorRes, null, null, 0, 0, null);
        }

        public static ImportResult ok(File file, String sha256, int w, int h, String ext) {
            return new ImportResult(true, 0, file, sha256, w, h, ext);
        }
    }

    private PlanImporter() {
    }

    public static String computeSha256Hex(@NonNull File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                md.update(buf, 0, r);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static ImportResult importFile(
            @NonNull Context context,
            @NonNull File sourceFile,
            @NonNull String mimeType,
            @NonNull File targetDir,
            @NonNull String targetFilePrefix
    ) {
        PlanImportValidator.Status typeStatus = PlanImportValidator.validateMimeType(mimeType);
        if (typeStatus != PlanImportValidator.Status.OK) {
            return ImportResult.error(R.string.import_err_type);
        }

        long sizeBytes = sourceFile.length();
        PlanImportValidator.Status sizeStatus = PlanImportValidator.validateFileSize(sizeBytes);
        if (sizeStatus != PlanImportValidator.Status.OK) {
            return ImportResult.error(R.string.import_err_size);
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        try {
            if ("application/pdf".equalsIgnoreCase(mimeType)) {
                return importPdf(sourceFile, targetDir, targetFilePrefix);
            } else {
                return importImage(sourceFile, mimeType, targetDir, targetFilePrefix);
            }
        } catch (OutOfMemoryError oom) {
            return ImportResult.error(R.string.import_err_decode);
        } catch (Exception e) {
            return ImportResult.error(R.string.import_err_decode);
        }
    }

    private static ImportResult importPdf(
            File sourceFile,
            File targetDir,
            String targetFilePrefix
    ) throws IOException {
        ParcelFileDescriptor pfd = null;
        PdfRenderer renderer = null;
        PdfRenderer.Page page = null;
        try {
            pfd = ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(pfd);
            if (renderer.getPageCount() == 0) {
                return ImportResult.error(R.string.import_err_pdf_empty);
            }
            page = renderer.openPage(0);
            int pageW = page.getWidth();
            int pageH = page.getHeight();

            PlanStoragePolicy.Decision decision = PlanStoragePolicy.decide("application/pdf", pageW, pageH, 1);
            int targetW = decision.targetWidth();
            int targetH = decision.targetHeight();

            Bitmap bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            File targetFile = new File(targetDir, targetFilePrefix + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
            }
            bitmap.recycle();

            String sha256 = computeSha256Hex(targetFile);
            return ImportResult.ok(targetFile, sha256, targetW, targetH, "png");
        } finally {
            if (page != null) {
                try {
                    page.close();
                } catch (Exception ignored) {
                }
            }
            if (renderer != null) {
                try {
                    renderer.close();
                } catch (Exception ignored) {
                }
            }
            if (pfd != null) {
                try {
                    pfd.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static ImportResult importImage(
            File sourceFile,
            String mimeType,
            File targetDir,
            String targetFilePrefix
    ) throws IOException {
        BitmapFactory.Options boundsOpts = new BitmapFactory.Options();
        boundsOpts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(sourceFile.getAbsolutePath(), boundsOpts);

        int origW = boundsOpts.outWidth;
        int origH = boundsOpts.outHeight;
        if (origW <= 0 || origH <= 0) {
            return ImportResult.error(R.string.import_err_decode);
        }

        PlanImportValidator.Status dimStatus = PlanImportValidator.validateDimensions(origW, origH);
        if (dimStatus == PlanImportValidator.Status.TOO_SMALL) {
            return ImportResult.error(R.string.import_err_small);
        } else if (dimStatus != PlanImportValidator.Status.OK) {
            return ImportResult.error(R.string.import_err_decode);
        }

        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try {
            ExifInterface exif = new ExifInterface(sourceFile.getAbsolutePath());
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (Exception ignored) {
        }

        PlanStoragePolicy.Decision decision = PlanStoragePolicy.decide(mimeType, origW, origH, orientation);

        if (decision.action() == PlanStoragePolicy.Action.STORE_ORIGINAL) {
            String ext = decision.targetFormat();
            File targetFile = new File(targetDir, targetFilePrefix + ".tmp");
            copyFile(sourceFile, targetFile);
            String sha256 = computeSha256Hex(targetFile);
            return ImportResult.ok(targetFile, sha256, origW, origH, ext);
        }

        // Re-encoding or rotation as PNG
        BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
        Bitmap srcBmp = BitmapFactory.decodeFile(sourceFile.getAbsolutePath(), decodeOpts);
        if (srcBmp == null) {
            return ImportResult.error(R.string.import_err_decode);
        }

        Bitmap finalBmp = srcBmp;
        int finalW = decision.targetWidth();
        int finalH = decision.targetHeight();

        if (decision.action() == PlanStoragePolicy.Action.REENCODE_ROTATE) {
            float rotDeg = 0.0f;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotDeg = 90.0f;
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotDeg = 180.0f;
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotDeg = 270.0f;

            Matrix matrix = new Matrix();
            matrix.postRotate(rotDeg);
            finalBmp = Bitmap.createBitmap(srcBmp, 0, 0, srcBmp.getWidth(), srcBmp.getHeight(), matrix, true);
            finalW = finalBmp.getWidth();
            finalH = finalBmp.getHeight();
        } else if (decision.action() == PlanStoragePolicy.Action.REENCODE) {
            finalBmp = Bitmap.createScaledBitmap(srcBmp, finalW, finalH, true);
        }

        File targetFile = new File(targetDir, targetFilePrefix + ".tmp");
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            finalBmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        }

        if (finalBmp != srcBmp) {
            finalBmp.recycle();
        }
        srcBmp.recycle();

        String sha256 = computeSha256Hex(targetFile);
        return ImportResult.ok(targetFile, sha256, finalW, finalH, "png");
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
            out.flush();
        }
    }
}
