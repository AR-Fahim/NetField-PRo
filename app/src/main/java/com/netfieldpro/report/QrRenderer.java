package com.netfieldpro.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import java.util.EnumMap;
import java.util.Map;

public final class QrRenderer {

    // 5 cm in PostScript points: (5.0 / 2.54) * 72 pt ≈ 141.732 pt
    public static final float MIN_SIZE_POINTS = (float) (5.0 / 2.54 * 72.0);

    private QrRenderer() {
    }

    public static QRCode encode(String content) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 0);
        return Encoder.encode(content, ErrorCorrectionLevel.M, hints);
    }

    public static int getVersion(String content) throws WriterException {
        QRCode code = encode(content);
        return code.getVersion().getVersionNumber();
    }

    public static void draw(Canvas canvas, QRCode qrCode, float left, float top, float size, Paint paint) {
        if (canvas == null || qrCode == null) {
            return;
        }
        ByteMatrix matrix = qrCode.getMatrix();
        if (matrix == null) {
            return;
        }
        Paint p = (paint != null) ? paint : new Paint();
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.BLACK);

        int cols = matrix.getWidth();
        int rows = matrix.getHeight();
        float cellW = size / cols;
        float cellH = size / rows;

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (matrix.get(x, y) != 0) {
                    canvas.drawRect(
                            left + x * cellW,
                            top + y * cellH,
                            left + (x + 1) * cellW,
                            top + (y + 1) * cellH,
                            p
                    );
                }
            }
        }
    }

    public static void draw(Canvas canvas, String content, float left, float top, float size, Paint paint) throws WriterException {
        QRCode code = encode(content);
        draw(canvas, code, left, top, size, paint);
    }
}
