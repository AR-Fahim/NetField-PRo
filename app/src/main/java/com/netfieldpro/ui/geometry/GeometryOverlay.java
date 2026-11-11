package com.netfieldpro.ui.geometry;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;

import androidx.annotation.NonNull;

import java.util.List;

import com.netfieldpro.core.geom.Pt;
import com.netfieldpro.core.model.entity.Wall;
import com.netfieldpro.core.model.enums.MaterialId;
import com.netfieldpro.core.scale.Affine3x3;
import com.netfieldpro.ui.plan.PlanView;

public class GeometryOverlay implements PlanView.OverlayDrawer {

    private final List<Pt> boundaryVertices;
    private final List<Wall> savedWalls;
    private final List<Pt> currentWallVertices;
    private final MaterialId currentMaterial;
    private final double mpp;
    private final float density;

    private final Paint boundaryLinePaint;
    private final Paint boundaryDotPaint;
    private final Paint wallLinePaint;
    private final Paint wallDotPaint;
    private final Paint inProgressWallPaint;

    public GeometryOverlay(
            List<Pt> boundaryVertices,
            List<Wall> savedWalls,
            List<Pt> currentWallVertices,
            MaterialId currentMaterial,
            double mpp,
            float density
    ) {
        this.boundaryVertices = boundaryVertices;
        this.savedWalls = savedWalls;
        this.currentWallVertices = currentWallVertices;
        this.currentMaterial = currentMaterial;
        this.mpp = mpp > 0 ? mpp : 0.025;
        this.density = density > 0 ? density : 1.0f;

        boundaryLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boundaryLinePaint.setStyle(Paint.Style.STROKE);
        boundaryLinePaint.setColor(Color.parseColor("#1976D2"));
        boundaryLinePaint.setStrokeWidth(3.0f * this.density);

        boundaryDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boundaryDotPaint.setStyle(Paint.Style.FILL);
        boundaryDotPaint.setColor(Color.parseColor("#1565C0"));

        wallLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wallLinePaint.setStyle(Paint.Style.STROKE);
        wallLinePaint.setStrokeWidth(4.0f * this.density);
        wallLinePaint.setStrokeCap(Paint.Cap.ROUND);

        wallDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wallDotPaint.setStyle(Paint.Style.FILL);

        inProgressWallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        inProgressWallPaint.setStyle(Paint.Style.STROKE);
        inProgressWallPaint.setStrokeWidth(3.0f * this.density);
        inProgressWallPaint.setPathEffect(new DashPathEffect(new float[]{10f * this.density, 6f * this.density}, 0f));
    }

    public static int getMaterialColor(MaterialId materialId) {
        if (materialId == null) return Color.DKGRAY;
        return switch (materialId) {
            case DRYWALL_LIGHT -> Color.parseColor("#9E9E9E");
            case GLASS_PARTITION -> Color.parseColor("#0288D1");
            case BRICK_MEDIUM -> Color.parseColor("#D84315");
            case CONCRETE_HEAVY -> Color.parseColor("#546E7A");
            case METAL_ELEVATOR -> Color.parseColor("#5E35B1");
        };
    }

    @Override
    public void onDrawOverlay(@NonNull Canvas canvas, @NonNull Affine3x3 imageToView) {
        float r = 5.0f * density;

        // 1. Draw saved walls
        if (savedWalls != null) {
            for (Wall w : savedWalls) {
                List<Wall.Vertex> verts = w.vertices();
                if (verts == null || verts.size() < 2) continue;
                int color = getMaterialColor(w.materialId());
                wallLinePaint.setColor(color);
                wallDotPaint.setColor(color);

                Path path = new Path();
                for (int i = 0; i < verts.size(); i++) {
                    Wall.Vertex v = verts.get(i);
                    float vx = (float) imageToView.transformX(v.x() / mpp, v.y() / mpp);
                    float vy = (float) imageToView.transformY(v.x() / mpp, v.y() / mpp);
                    if (i == 0) {
                        path.moveTo(vx, vy);
                    } else {
                        path.lineTo(vx, vy);
                    }
                    canvas.drawCircle(vx, vy, r, wallDotPaint);
                }
                canvas.drawPath(path, wallLinePaint);
            }
        }

        // 2. Draw in-progress wall
        if (currentWallVertices != null && !currentWallVertices.isEmpty()) {
            int color = getMaterialColor(currentMaterial);
            inProgressWallPaint.setColor(color);
            wallDotPaint.setColor(color);

            Path path = new Path();
            for (int i = 0; i < currentWallVertices.size(); i++) {
                Pt p = currentWallVertices.get(i);
                float vx = (float) imageToView.transformX(p.x() / mpp, p.y() / mpp);
                float vy = (float) imageToView.transformY(p.x() / mpp, p.y() / mpp);
                if (i == 0) {
                    path.moveTo(vx, vy);
                } else {
                    path.lineTo(vx, vy);
                }
                canvas.drawCircle(vx, vy, r, wallDotPaint);
            }
            if (currentWallVertices.size() >= 2) {
                canvas.drawPath(path, inProgressWallPaint);
            }
        }

        // 3. Draw boundary
        if (boundaryVertices != null && !boundaryVertices.isEmpty()) {
            Path path = new Path();
            for (int i = 0; i < boundaryVertices.size(); i++) {
                Pt p = boundaryVertices.get(i);
                float vx = (float) imageToView.transformX(p.x() / mpp, p.y() / mpp);
                float vy = (float) imageToView.transformY(p.x() / mpp, p.y() / mpp);
                if (i == 0) {
                    path.moveTo(vx, vy);
                } else {
                    path.lineTo(vx, vy);
                }
                canvas.drawCircle(vx, vy, r, boundaryDotPaint);
            }
            if (boundaryVertices.size() >= 3) {
                path.close();
            }
            canvas.drawPath(path, boundaryLinePaint);
        }
    }
}

