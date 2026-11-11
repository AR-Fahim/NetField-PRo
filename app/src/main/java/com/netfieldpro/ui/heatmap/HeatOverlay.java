package com.netfieldpro.ui.heatmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import com.netfieldpro.core.heat.IdwSurface;
import com.netfieldpro.core.heat.Palette;
import com.netfieldpro.core.heat.PaletteId;
import com.netfieldpro.core.scale.Affine3x3;
import com.netfieldpro.data.repository.SurveyReadRepository.PointRecord;
import com.netfieldpro.ui.plan.PlanView;

public class HeatOverlay implements PlanView.OverlayDrawer {

    private final Bitmap heatBitmap;
    private final double originX;
    private final double originY;
    private final double g;
    private final int cols;
    private final int rows;
    private final double mpp;
    private final PaletteId paletteId;
    private final List<PointRecord> points;
    private final List<Double> pointDisplayValues;
    private final float density;

    private final Paint bitmapPaint;
    private final Paint pinCirclePaint;
    private final Paint pinTextPaint;

    public HeatOverlay(
            @Nullable Bitmap heatBitmap,
            double originX,
            double originY,
            double g,
            int cols,
            int rows,
            double mpp,
            PaletteId paletteId,
            List<PointRecord> points,
            List<Double> pointDisplayValues,
            float density
    ) {
        this.heatBitmap = heatBitmap;
        this.originX = originX;
        this.originY = originY;
        this.g = g;
        this.cols = cols;
        this.rows = rows;
        this.mpp = mpp;
        this.paletteId = (paletteId != null) ? paletteId : PaletteId.TRAFFIC_LIGHT;
        this.points = points;
        this.pointDisplayValues = pointDisplayValues;
        this.density = (density > 0) ? density : 1.0f;

        this.bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        this.pinCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.pinCirclePaint.setStyle(Paint.Style.FILL);

        this.pinTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.pinTextPaint.setTextAlign(Paint.Align.CENTER);
        this.pinTextPaint.setTextSize(11.0f * this.density);
        this.pinTextPaint.setFakeBoldText(true);
    }

    public static Bitmap createHeatBitmap(
            IdwSurface surface,
            int cols,
            int rows,
            PaletteId paletteId,
            boolean isDarkTheme
    ) {
        if (surface == null || cols <= 0 || rows <= 0) {
            return null;
        }
        int alpha = isDarkTheme ? (int) Math.round(255 * 0.75) : (int) Math.round(255 * 0.65);
        int total = cols * rows;
        int[] pixels = new int[total];
        double[] dbm = surface.dbm();
        byte[] cellState = surface.cellState();

        for (int i = 0; i < total; i++) {
            if (cellState[i] == IdwSurface.VALUE) {
                int[] rgb = Palette.rgb(paletteId, dbm[i]);
                pixels[i] = Color.argb(alpha, rgb[0], rgb[1], rgb[2]);
            } else {
                pixels[i] = Color.TRANSPARENT;
            }
        }
        return Bitmap.createBitmap(pixels, cols, rows, Bitmap.Config.ARGB_8888);
    }

    @Override
    public void onDrawOverlay(@NonNull Canvas canvas, @NonNull Affine3x3 imageToView) {
        if (heatBitmap != null && mpp > 0.0) {
            Matrix m = new Matrix();
            m.setValues(new float[]{
                    (float) imageToView.getM00(), (float) imageToView.getM01(), (float) imageToView.getM02(),
                    (float) imageToView.getM10(), (float) imageToView.getM11(), (float) imageToView.getM12(),
                    0f, 0f, 1f
            });

            canvas.save();
            canvas.concat(m);
            float left = (float) (originX / mpp);
            float top = (float) (originY / mpp);
            float right = (float) ((originX + cols * g) / mpp);
            float bottom = (float) ((originY + rows * g) / mpp);
            Rect src = new Rect(0, 0, cols, rows);
            RectF dst = new RectF(left, top, right, bottom);
            canvas.drawBitmap(heatBitmap, src, dst, bitmapPaint);
            canvas.restore();
        }

        if (points != null && !points.isEmpty() && mpp > 0.0) {
            double s = Math.hypot(imageToView.getM00(), imageToView.getM10());
            double zoomDpPerM = (s / mpp) / (density > 0 ? density : 1.0);
            boolean showGlyphs = zoomDpPerM >= 0.5;

            for (int i = 0; i < points.size(); i++) {
                PointRecord pr = points.get(i);
                double px = pr.point().x() / mpp;
                double py = pr.point().y() / mpp;
                float vx = (float) imageToView.transformX(px, py);
                float vy = (float) imageToView.transformY(px, py);

                double val = (pointDisplayValues != null && i < pointDisplayValues.size())
                        ? pointDisplayValues.get(i)
                        : -95.0;

                int[] rgb = Palette.rgb(paletteId, val);
                int circleColor = Color.rgb(rgb[0], rgb[1], rgb[2]);
                pinCirclePaint.setColor(circleColor);

                if (showGlyphs) {
                    float r = 14.0f * density;
                    canvas.drawCircle(vx, vy, r, pinCirclePaint);

                    int textColor = Palette.textOn(rgb);
                    pinTextPaint.setColor(textColor);
                    String label = String.valueOf((int) Math.round(val));
                    float textHeight = pinTextPaint.descent() - pinTextPaint.ascent();
                    float textOffset = (textHeight / 2) - pinTextPaint.descent();
                    canvas.drawText(label, vx, vy + textOffset, pinTextPaint);
                } else {
                    float r = 5.0f * density;
                    canvas.drawCircle(vx, vy, r, pinCirclePaint);
                }
            }
        }
    }
}

