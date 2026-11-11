package com.netfieldpro.report;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import androidx.annotation.NonNull;

import com.netfieldpro.core.model.enums.Verdict;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

public final class PdfRenderer {

    public static final int PAGE_WIDTH_PT = 595;
    public static final int PAGE_HEIGHT_PT = 842;
    private static final int BRAND_COLOR = Color.parseColor("#036434");

    private PdfRenderer() {
    }

    public static void renderToFile(@NonNull ReportModel model, @NonNull File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            renderToStream(model, fos);
        }
    }

    public static void renderToStream(@NonNull ReportModel model, @NonNull OutputStream out) throws IOException {
        PdfDocument document = renderToDocument(model);
        try {
            document.writeTo(out);
        } finally {
            document.close();
        }
    }

    public static PdfDocument renderToDocument(@NonNull ReportModel model) {
        PdfDocument document = new PdfDocument();
        int totalPages = model.totalPages();

        if (model.isSigned()) {
            // Signed: 4 pages
            renderPage1(document, model, 1, totalPages);
            renderPage2(document, model, 2, totalPages);
            renderPage3(document, model, 3, totalPages);
            renderPage4(document, model, 4, totalPages);
        } else {
            // Sample: 2 pages (Cover and Floor+Heat)
            renderPage1(document, model, 1, totalPages);
            renderPage3(document, model, 2, totalPages);
        }

        return document;
    }

    // --- Page 1: Cover ---
    private static void renderPage1(PdfDocument doc, ReportModel model, int pageNum, int totalPages) {
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT, PAGE_HEIGHT_PT, pageNum).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        drawHeaderBand(canvas, model.reportTitle());

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTextSize(22f);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setColor(Color.DKGRAY);
        canvas.drawText(model.reportTitle(), 24f, 95f, titlePaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(13f);
        textPaint.setColor(Color.BLACK);

        float y = 140f;
        canvas.drawText("Project: " + model.projectName(), 24f, y, textPaint);
        y += 25f;
        canvas.drawText("Site: " + model.siteName(), 24f, y, textPaint);
        y += 25f;
        canvas.drawText("Floor: " + model.floorName(), 24f, y, textPaint);
        y += 25f;
        canvas.drawText(model.formattedDate(), 24f, y, textPaint);

        if (model.isSigned() && model.verdict() != null) {
            y += 45f;
            drawVerdictChip(canvas, 24f, y, model.verdict(), model.verdictLabel());
            y += 65f;
        } else {
            y += 40f;
        }

        canvas.drawText("Evidence Grade: " + model.gradeLabel(), 24f, y, textPaint);
        y += 25f;
        String countsLine = String.format("Survey Points: %d  ·  Low Confidence: %d  ·  Moved: %d",
                model.pointsCount(), model.lowConfidenceCount(), model.movedCount());
        canvas.drawText(countsLine, 24f, y, textPaint);

        if (!model.isSigned() && model.watermarkText() != null) {
            drawWatermark(canvas, model.watermarkText());
        }

        drawFooter(canvas, pageNum, totalPages, model.shortSessionId());
        doc.finishPage(page);
    }

    // --- Page 2: Criteria Table ---
    private static void renderPage2(PdfDocument doc, ReportModel model, int pageNum, int totalPages) {
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT, PAGE_HEIGHT_PT, pageNum).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        drawHeaderBand(canvas, "Executive Summary · Criteria Table");

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTextSize(14f);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setColor(Color.BLACK);
        String subHeader = (model.verdictLabel() != null ? model.verdictLabel() : "Verdict") + "  ·  Target Requirement Profile";
        canvas.drawText(subHeader, 24f, 80f, titlePaint);

        // Table Header
        float tableTop = 110f;
        Paint headerBg = new Paint();
        headerBg.setColor(Color.parseColor("#F0F4F8"));
        canvas.drawRect(24f, tableTop, 571f, tableTop + 30f, headerBg);

        Paint thPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thPaint.setTextSize(11f);
        thPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        thPaint.setColor(Color.DKGRAY);

        canvas.drawText("Criterion", 32f, tableTop + 20f, thPaint);
        canvas.drawText("Required", 170f, tableTop + 20f, thPaint);
        canvas.drawText("Measured", 310f, tableTop + 20f, thPaint);
        canvas.drawText("Status", 450f, tableTop + 20f, thPaint);

        Paint rowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rowPaint.setTextSize(10.5f);
        rowPaint.setColor(Color.BLACK);

        Paint divPaint = new Paint();
        divPaint.setColor(Color.parseColor("#E0E0E0"));
        divPaint.setStrokeWidth(1f);

        float rowY = tableTop + 30f;
        List<ReportModel.CriterionRow> rows = model.criteriaRows();
        for (int i = 0; i < rows.size(); i++) {
            ReportModel.CriterionRow row = rows.get(i);
            float currentY = rowY + (i * 35f);

            if (i % 2 == 1) {
                Paint zebra = new Paint();
                zebra.setColor(Color.parseColor("#FAFAFA"));
                canvas.drawRect(24f, currentY, 571f, currentY + 35f, zebra);
            }

            canvas.drawLine(24f, currentY, 571f, currentY, divPaint);

            rowPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(row.name(), 32f, currentY + 22f, rowPaint);

            rowPaint.setTypeface(Typeface.DEFAULT);
            canvas.drawText(row.required(), 170f, currentY + 22f, rowPaint);
            canvas.drawText(row.measured(), 310f, currentY + 22f, rowPaint);

            Paint statusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            statusPaint.setTextSize(10.5f);
            statusPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            if (row.isMet()) {
                statusPaint.setColor(Color.parseColor("#2E7D32"));
            } else if (row.statusText().contains("Not met")) {
                statusPaint.setColor(Color.parseColor("#C62828"));
            } else {
                statusPaint.setColor(Color.GRAY);
            }
            canvas.drawText(row.statusText(), 450f, currentY + 22f, statusPaint);
        }

        canvas.drawLine(24f, rowY + rows.size() * 35f, 571f, rowY + rows.size() * 35f, divPaint);

        drawFooter(canvas, pageNum, totalPages, model.shortSessionId());
        doc.finishPage(page);
    }

    // --- Page 3: Floor Plan & Coverage Heatmap ---
    private static void renderPage3(PdfDocument doc, ReportModel model, int pageNum, int totalPages) {
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT, PAGE_HEIGHT_PT, pageNum).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        drawHeaderBand(canvas, "Floor Plan & Coverage Heatmap");

        Paint infoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        infoPaint.setTextSize(11f);
        infoPaint.setColor(Color.BLACK);
        String info = model.floorName() + "  ·  Palette: " + model.paletteName();
        canvas.drawText(info, 24f, 75f, infoPaint);

        // Draw Composite Plan Bitmap
        Bitmap plan = model.compositePlanBitmap();
        float boxLeft = 24f;
        float boxTop = 90f;
        float boxRight = 571f;
        float boxBottom = 500f;
        float boxW = boxRight - boxLeft;
        float boxH = boxBottom - boxTop;

        if (plan != null) {
            float planW = plan.getWidth();
            float planH = plan.getHeight();
            float scale = Math.min(boxW / planW, boxH / planH);
            float drawW = planW * scale;
            float drawH = planH * scale;
            float drawLeft = boxLeft + (boxW - drawW) / 2f;
            float drawTop = boxTop + (boxH - drawH) / 2f;

            Rect src = new Rect(0, 0, plan.getWidth(), plan.getHeight());
            RectF dst = new RectF(drawLeft, drawTop, drawLeft + drawW, drawTop + drawH);
            Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(plan, src, dst, p);

            Paint borderPaint = new Paint();
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(1f);
            borderPaint.setColor(Color.LTGRAY);
            canvas.drawRect(dst, borderPaint);
        }

        // Draw Legend
        float legendY = 530f;
        List<ReportModel.LegendEntry> legends = model.legendEntries();
        if (legends != null && !legends.isEmpty()) {
            Paint sqPaint = new Paint();
            sqPaint.setStyle(Paint.Style.FILL);

            Paint lblPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            lblPaint.setTextSize(10f);
            lblPaint.setColor(Color.DKGRAY);

            float curX = 24f;
            for (ReportModel.LegendEntry le : legends) {
                sqPaint.setColor(le.color());
                canvas.drawRect(curX, legendY, curX + 14f, legendY + 14f, sqPaint);
                canvas.drawText(le.label(), curX + 18f, legendY + 11f, lblPaint);
                curX += 95f;
            }
        }

        // Interp note
        Paint notePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        notePaint.setTextSize(10f);
        notePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        notePaint.setColor(Color.GRAY);
        canvas.drawText(model.interpNote(), 24f, 570f, notePaint);

        if (!model.isSigned()) {
            Paint unsignedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            unsignedPaint.setTextSize(12f);
            unsignedPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            unsignedPaint.setColor(Color.parseColor("#C62828"));
            canvas.drawText("Unsigned — Not for contractual audit", 24f, 620f, unsignedPaint);

            if (model.watermarkText() != null) {
                drawWatermark(canvas, model.watermarkText());
            }
        }

        drawFooter(canvas, pageNum, totalPages, model.shortSessionId());
        doc.finishPage(page);
    }

    // --- Page 4: Integrity ---
    private static void renderPage4(PdfDocument doc, ReportModel model, int pageNum, int totalPages) {
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT, PAGE_HEIGHT_PT, pageNum).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        drawHeaderBand(canvas, "Integrity & Cryptographic Verification");

        // Vector QR Code (at least 5 cm square: 150 pt x 150 pt)
        float qrSize = 150f;
        float qrLeft = 24f;
        float qrTop = 75f;
        if (model.qrFullJson() != null) {
            try {
                QrRenderer.draw(canvas, model.qrFullJson(), qrLeft, qrTop, qrSize, null);
            } catch (Exception ignored) {
            }
        }

        // Side notes next to QR
        Paint sideTitle = new Paint(Paint.ANTI_ALIAS_FLAG);
        sideTitle.setTextSize(12f);
        sideTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        sideTitle.setColor(Color.BLACK);
        canvas.drawText("Cryptographic Verification QR", 195f, 95f, sideTitle);

        Paint sideText = new Paint(Paint.ANTI_ALIAS_FLAG);
        sideText.setTextSize(9.5f);
        sideText.setColor(Color.DKGRAY);
        canvas.drawText("Scan with NetField Pro or any WebCrypto verifier.", 195f, 115f, sideText);
        canvas.drawText("Contains signed assessment digest, criteria, and ECDSA signature.", 195f, 130f, sideText);
        canvas.drawText("Payload version ≤ 16, Error Correction Level M.", 195f, 145f, sideText);

        // Monospace Hashes & Signatures
        float fieldY = 250f;
        Paint monoLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        monoLabel.setTextSize(10f);
        monoLabel.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        monoLabel.setColor(Color.BLACK);

        Paint monoVal = new Paint(Paint.ANTI_ALIAS_FLAG);
        monoVal.setTextSize(8.5f);
        monoVal.setTypeface(Typeface.MONOSPACE);
        monoVal.setColor(Color.parseColor("#1B2A4A"));

        canvas.drawText("Evidence Hash (eh):", 24f, fieldY, monoLabel);
        fieldY += 14f;
        canvas.drawText(model.evidenceHash() != null ? model.evidenceHash() : "–", 24f, fieldY, monoVal);

        fieldY += 24f;
        canvas.drawText("Assessment Hash (ah):", 24f, fieldY, monoLabel);
        fieldY += 14f;
        canvas.drawText(model.assessmentHash() != null ? model.assessmentHash() : "–", 24f, fieldY, monoVal);

        fieldY += 24f;
        canvas.drawText("ECDSA P-256 Signature (sg):", 24f, fieldY, monoLabel);
        fieldY += 14f;
        canvas.drawText(model.signature() != null ? model.signature() : "–", 24f, fieldY, monoVal);

        fieldY += 24f;
        canvas.drawText("Signing Key Fingerprint (key fp):", 24f, fieldY, monoLabel);
        fieldY += 14f;
        canvas.drawText(model.publicKeyFp() != null ? model.publicKeyFp() : "–", 24f, fieldY, monoVal);

        // Versions
        fieldY += 30f;
        Paint verPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        verPaint.setTextSize(9.5f);
        verPaint.setColor(Color.DKGRAY);
        canvas.drawText(String.format("App Version: %s  ·  Algorithm Version: %s", model.appVersion(), model.algoVersion()), 24f, fieldY, verPaint);

        // Instructions Block
        fieldY += 25f;
        float boxHeight = 110f;
        Paint boxBg = new Paint();
        boxBg.setColor(Color.parseColor("#F4F6F8"));
        canvas.drawRect(24f, fieldY, 571f, fieldY + boxHeight, boxBg);

        Paint boxBorder = new Paint();
        boxBorder.setStyle(Paint.Style.STROKE);
        boxBorder.setStrokeWidth(1f);
        boxBorder.setColor(Color.parseColor("#D0D7DE"));
        canvas.drawRect(24f, fieldY, 571f, fieldY + boxHeight, boxBorder);

        Paint insTitle = new Paint(Paint.ANTI_ALIAS_FLAG);
        insTitle.setTextSize(10f);
        insTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        insTitle.setColor(Color.BLACK);
        canvas.drawText("Independent Verification (Level 2):", 34f, fieldY + 20f, insTitle);

        Paint insCode = new Paint(Paint.ANTI_ALIAS_FLAG);
        insCode.setTextSize(8.5f);
        insCode.setTypeface(Typeface.MONOSPACE);
        insCode.setColor(Color.parseColor("#24292F"));
        canvas.drawText("1. Verify evidence manifest: sha256sum -c manifest.sha256", 34f, fieldY + 42f, insCode);
        canvas.drawText("2. Verify signature: openssl dgst -sha256 -verify pubkey.pem -signature sig.raw manifest.json", 34f, fieldY + 62f, insCode);
        canvas.drawText("3. Extract public key: openssl ec -pubin -inform DER -in pubkey.der", 34f, fieldY + 82f, insCode);

        // Debug note
        if (model.debugNote() != null) {
            Paint debugPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            debugPaint.setTextSize(10f);
            debugPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            debugPaint.setColor(Color.parseColor("#B71C1C"));
            canvas.drawText(model.debugNote(), 24f, fieldY + boxHeight + 35f, debugPaint);
        }

        drawFooter(canvas, pageNum, totalPages, model.shortSessionId());
        doc.finishPage(page);
    }

    private static void drawHeaderBand(Canvas canvas, String title) {
        Paint bandPaint = new Paint();
        bandPaint.setColor(BRAND_COLOR);
        canvas.drawRect(0f, 0f, PAGE_WIDTH_PT, 44f, bandPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(14f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setColor(Color.WHITE);
        canvas.drawText("NetField Pro  ·  " + title, 24f, 28f, textPaint);
    }

    private static void drawFooter(Canvas canvas, int pageNum, int totalPages, String shortSessionId) {
        Paint divPaint = new Paint();
        divPaint.setColor(Color.parseColor("#DCDCDC"));
        divPaint.setStrokeWidth(1f);
        canvas.drawLine(24f, 805f, 571f, 805f, divPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(9f);
        textPaint.setColor(Color.GRAY);
        String footerText = String.format(Locale.US, "Page %d/%d  ·  Session %s", pageNum, totalPages, shortSessionId);
        canvas.drawText(footerText, 24f, 822f, textPaint);
    }

    private static void drawVerdictChip(Canvas canvas, float left, float top, Verdict verdict, String label) {
        float width = 160f;
        float height = 40f;
        RectF rect = new RectF(left, top, left + width, top + height);

        Paint chipBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        switch (verdict) {
            case PASS:
                chipBg.setColor(Color.parseColor("#2E7D32"));
                break;
            case FAIL:
                chipBg.setColor(Color.parseColor("#C62828"));
                break;
            case INCOMPLETE:
                chipBg.setColor(Color.parseColor("#EF6C00"));
                break;
            case NOT_ELIGIBLE:
            default:
                chipBg.setColor(Color.parseColor("#616161"));
                break;
        }
        canvas.drawRoundRect(rect, 8f, 8f, chipBg);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(15f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);

        float textX = rect.centerX();
        float textY = rect.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2);
        canvas.drawText(label != null ? label : verdict.name(), textX, textY, textPaint);
    }

    private static void drawWatermark(Canvas canvas, String watermark) {
        canvas.save();
        canvas.rotate(-45f, PAGE_WIDTH_PT / 2f, PAGE_HEIGHT_PT / 2f);

        Paint wmPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wmPaint.setColor(Color.argb(35, 180, 0, 0));
        wmPaint.setTextSize(22f);
        wmPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        wmPaint.setTextAlign(Paint.Align.CENTER);

        canvas.drawText(watermark, PAGE_WIDTH_PT / 2f, PAGE_HEIGHT_PT / 2f, wmPaint);
        canvas.restore();
    }
}
