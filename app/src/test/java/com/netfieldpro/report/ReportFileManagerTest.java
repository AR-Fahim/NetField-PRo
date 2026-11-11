package com.netfieldpro.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ReportFileManagerTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testGenerateFileNameSanitized() {
        // Site present
        long ts = 1794528000000L; // 2026-11-12
        String fn1 = ReportFileManager.generateFileName("HQ", "Demo Office", ts);
        assertTrue(fn1.startsWith("NetField-HQ-"));
        assertTrue(fn1.endsWith(".pdf"));

        // Site empty, project present
        String fn2 = ReportFileManager.generateFileName("", "Demo Office", ts);
        assertTrue(fn2.startsWith("NetField-Demo_Office-"));
        assertTrue(fn2.endsWith(".pdf"));

        // Non-ASCII and special characters sanitized
        String fn3 = ReportFileManager.generateFileName("Site/Building #1 (Main)!", "Demo", ts);
        assertFalse(fn3.contains("/"));
        assertFalse(fn3.contains("#"));
        assertFalse(fn3.contains("("));
        assertFalse(fn3.contains("!"));
        assertTrue(fn3.startsWith("NetField-Site_Building_1_Main-"));
        assertTrue(fn3.endsWith(".pdf"));
    }

    @Test
    public void testCleanOldReports() throws Exception {
        File reportsDir = ReportFileManager.getReportsDir(context);
        assertTrue(reportsDir.exists());

        long now = 200_000_000L;
        File oldFile = new File(reportsDir, "old_report.pdf");
        assertTrue(oldFile.createNewFile());
        assertTrue(oldFile.setLastModified(now - (ReportFileManager.ONE_DAY_MS + 5000L)));

        File newFile = new File(reportsDir, "new_report.pdf");
        assertTrue(newFile.createNewFile());
        assertTrue(newFile.setLastModified(now - 1000L));

        int deleted = ReportFileManager.cleanOldReports(context, now);
        assertEquals(1, deleted);
        assertFalse(oldFile.exists());
        assertTrue(newFile.exists());

        newFile.delete();
    }

    @Test
    public void testCreateShareAndViewIntents() throws Exception {
        File reportsDir = ReportFileManager.getReportsDir(context);
        File pdfFile = new File(reportsDir, "share_test.pdf");
        assertTrue(pdfFile.createNewFile());

        Uri fileUri = ReportFileManager.getFileUri(context, pdfFile);
        assertNotNull(fileUri);
        assertTrue(fileUri.toString().contains("share_test.pdf"));

        Intent shareChooser = ReportFileManager.createShareIntent(context, pdfFile);
        assertNotNull(shareChooser);
        assertEquals(Intent.ACTION_CHOOSER, shareChooser.getAction());

        Intent viewIntent = ReportFileManager.createViewIntent(context, pdfFile);
        assertNotNull(viewIntent);
        assertEquals(Intent.ACTION_VIEW, viewIntent.getAction());
        assertEquals("application/pdf", viewIntent.getType());
        assertTrue((viewIntent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);

        pdfFile.delete();
    }
}

