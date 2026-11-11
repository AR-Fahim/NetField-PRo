package com.netfieldpro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;

import com.netfieldpro.data.plan.PlanImporter;

@RunWith(AndroidJUnit4.class)
public class PlanImportDeviceTest {

    private Context context;
    private File testDir;
    private File targetDir;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        testDir = new File(context.getCacheDir(), "device_plan_test_src");
        targetDir = new File(context.getCacheDir(), "device_plan_test_dst");
        testDir.mkdirs();
        targetDir.mkdirs();
    }

    @After
    public void tearDown() {
        deleteRecursive(testDir);
        deleteRecursive(targetDir);
    }

    private void deleteRecursive(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) deleteRecursive(f);
                    else f.delete();
                }
            }
            dir.delete();
        }
    }

    @Test
    public void testLargePngReencodedTo4096x1638() throws Exception {
        // 5000x2000 PNG fixture generated at test time
        File sourceFile = new File(testDir, "fixture_5000x2000.png");
        Bitmap bmp = Bitmap.createBitmap(5000, 2000, Bitmap.Config.ARGB_8888);
        try (FileOutputStream fos = new FileOutputStream(sourceFile)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        }
        bmp.recycle();

        PlanImporter.ImportResult result = PlanImporter.importFile(
                context,
                sourceFile,
                "image/png",
                targetDir,
                "reencode_result"
        );

        assertTrue("Import must succeed", result.success());
        assertEquals("Width must be scaled to 4096", 4096, result.width());
        assertEquals("Height must be scaled to 1638", 1638, result.height());
        assertEquals("Extension must be png", "png", result.extension());

        assertNotNull(result.storedFile());
        assertTrue(result.storedFile().exists());

        BitmapFactory.Options boundsOpts = new BitmapFactory.Options();
        boundsOpts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(result.storedFile().getAbsolutePath(), boundsOpts);
        assertEquals(4096, boundsOpts.outWidth);
        assertEquals(1638, boundsOpts.outHeight);
    }

    @Test
    public void testMediumPngStoredByteForByte() throws Exception {
        // 3000x2000 PNG fixture stored byte-for-byte
        File sourceFile = new File(testDir, "fixture_3000x2000.png");
        Bitmap bmp = Bitmap.createBitmap(3000, 2000, Bitmap.Config.ARGB_8888);
        try (FileOutputStream fos = new FileOutputStream(sourceFile)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        }
        bmp.recycle();

        String sourceHash = PlanImporter.computeSha256Hex(sourceFile);
        long sourceLength = sourceFile.length();

        PlanImporter.ImportResult result = PlanImporter.importFile(
                context,
                sourceFile,
                "image/png",
                targetDir,
                "byte_for_byte_result"
        );

        assertTrue("Import must succeed", result.success());
        assertEquals(3000, result.width());
        assertEquals(2000, result.height());
        assertEquals("png", result.extension());
        assertNotNull(result.storedFile());
        assertTrue(result.storedFile().exists());

        assertEquals("Hash must equal source hash byte-for-byte", sourceHash, result.sha256());
        assertEquals("File size must equal source file size byte-for-byte", sourceLength, result.storedFile().length());
    }

    @Test
    public void testOnePagePdfRasterizedAtSp5Target() throws Exception {
        // One-page PDF with page 612x792 pt
        File sourceFile = new File(testDir, "fixture_612x792.pdf");
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(612, 792, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        page.getCanvas().drawColor(Color.WHITE);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3.0f);
        page.getCanvas().drawRect(50, 50, 562, 742, paint);
        doc.finishPage(page);

        try (FileOutputStream fos = new FileOutputStream(sourceFile)) {
            doc.writeTo(fos);
            fos.flush();
        }
        doc.close();

        PlanImporter.ImportResult result = PlanImporter.importFile(
                context,
                sourceFile,
                "application/pdf",
                targetDir,
                "pdf_result"
        );

        assertTrue("Import must succeed", result.success());
        // SP-5 target for 612x792 is 2374x3072
        assertEquals("Target width must be 2374", 2374, result.width());
        assertEquals("Target height must be 3072", 3072, result.height());
        assertEquals("Extension must be png", "png", result.extension());

        assertNotNull(result.storedFile());
        assertTrue(result.storedFile().exists());

        BitmapFactory.Options boundsOpts = new BitmapFactory.Options();
        boundsOpts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(result.storedFile().getAbsolutePath(), boundsOpts);
        assertEquals(2374, boundsOpts.outWidth);
        assertEquals(3072, boundsOpts.outHeight);
    }
}
