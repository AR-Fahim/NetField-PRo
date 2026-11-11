package com.netfieldpro.ui.plan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.netfieldpro.core.plan.TilePlanner;
import com.netfieldpro.core.plan.ViewportController;
import com.netfieldpro.core.scale.Affine3x3;

public class PlanView extends View {

    public interface OverlayDrawer {
        void onDrawOverlay(@NonNull Canvas canvas, @NonNull Affine3x3 imageToView);
    }

    public interface HandleMoveListener {
        void onHandleMoved(int handleIndex, double x, double y);
    }

    public interface TileDecodeListener {
        void onTilePlanComputed(@NonNull TilePlanner.TilePlan plan);
        void onTileDecodeRequested(@NonNull TilePlanner.TilePlan plan);
        void onTileDecodeCancelled();
        void onTileDecoded(@NonNull Bitmap tile);
    }

    public interface PlanTapListener {
        void onPlanTapped(double imgX, double imgY);
    }

    public interface PinDragListener {
        void onPinDragged(double imgX, double imgY);
    }

    private static final ExecutorService PLAN_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "nf-plan");
        t.setDaemon(true);
        return t;
    });

    private final ViewportController viewport = new ViewportController();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Matrix drawMatrix = new Matrix();
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handleInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int imageW;
    private int imageH;
    private int overviewLong;

    private Bitmap overviewBitmap;
    private Bitmap tileBitmap;
    private BitmapRegionDecoder regionDecoder;
    private TilePlanner.TilePlan currentTilePlan;
    private Future<?> currentDecodeTask;

    private OverlayDrawer overlayDrawer;
    private HandleMoveListener handleMoveListener;
    private TileDecodeListener tileDecodeListener;
    private PlanTapListener planTapListener;
    private PinDragListener pinDragListener;

    private boolean hasHandles = false;
    private double handle1X = -1.0;
    private double handle1Y = -1.0;
    private double handle2X = -1.0;
    private double handle2Y = -1.0;
    private int activeHandle = 0; // 0: none, 1: handle 1, 2: handle 2

    private boolean hasDraggablePin = false;
    private double draggablePinX = -1.0;
    private double draggablePinY = -1.0;
    private boolean activePinDrag = false;

    private float lastTouchX;
    private float lastTouchY;
    private float downTouchX;
    private float downTouchY;
    private boolean movedBeyondSlop = false;
    private boolean isDragging = false;
    private ScaleGestureDetector scaleDetector;
    private PlanAccessibilityHelper accessibilityHelper;

    private final Runnable settleRunnable = this::onGesturesSettled;

    public PlanView(Context context) {
        super(context);
        init();
    }

    public PlanView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PlanView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                viewport.zoomAt(factor, detector.getFocusX(), detector.getFocusY());
                invalidate();
                return true;
            }
        });

        handlePaint.setColor(0xFF1E88E5); // Blue
        handlePaint.setStyle(Paint.Style.STROKE);
        handlePaint.setStrokeWidth(6.0f);

        handleInnerPaint.setColor(0xFFFFFFFF);
        handleInnerPaint.setStyle(Paint.Style.FILL);

        linePaint.setColor(0xFF1E88E5);
        linePaint.setStrokeWidth(4.0f);
        linePaint.setStyle(Paint.Style.STROKE);

        setContentDescription("Floor plan");

        accessibilityHelper = new PlanAccessibilityHelper(this);
        ViewCompat.setAccessibilityDelegate(this, accessibilityHelper);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (imageW > 0 && imageH > 0 && w > 0 && h > 0) {
            viewport.setDimensions(imageW, imageH, w, h);
            invalidate();
        }
    }

    public void setImageDimensions(int w, int h) {
        this.imageW = w;
        this.imageH = h;
        this.overviewLong = Math.max(w, h);
        if (getWidth() > 0 && getHeight() > 0) {
            viewport.setDimensions(imageW, imageH, getWidth(), getHeight());
        }
        invalidate();
    }

    public void setOverviewBitmap(@NonNull Bitmap bitmap, int fullW, int fullH) {
        this.overviewBitmap = bitmap;
        this.imageW = fullW;
        this.imageH = fullH;
        this.overviewLong = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (getWidth() > 0 && getHeight() > 0) {
            viewport.setDimensions(imageW, imageH, getWidth(), getHeight());
        }
        invalidate();
    }

    public void loadPlanFile(@NonNull File file) {
        try {
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), boundsOptions);
            this.imageW = boundsOptions.outWidth;
            this.imageH = boundsOptions.outHeight;

            int maxSide = Math.max(imageW, imageH);
            int sampleSize = 1;
            while (maxSide / sampleSize > 2048) {
                sampleSize *= 2;
            }

            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = sampleSize;
            this.overviewBitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), decodeOptions);
            this.overviewLong = Math.max(overviewBitmap.getWidth(), overviewBitmap.getHeight());

            this.regionDecoder = BitmapRegionDecoder.newInstance(file.getAbsolutePath(), false);

            if (getWidth() > 0 && getHeight() > 0) {
                viewport.setDimensions(imageW, imageH, getWidth(), getHeight());
            }
            invalidate();
        } catch (Exception ignored) {
        }
    }

    public void setRegionDecoder(@Nullable BitmapRegionDecoder decoder) {
        this.regionDecoder = decoder;
    }

    public void setOverlayDrawer(@Nullable OverlayDrawer drawer) {
        this.overlayDrawer = drawer;
        invalidate();
    }

    public void setHandleMoveListener(@Nullable HandleMoveListener listener) {
        this.handleMoveListener = listener;
    }

    public void setTileDecodeListener(@Nullable TileDecodeListener listener) {
        this.tileDecodeListener = listener;
    }

    public void setPlanTapListener(@Nullable PlanTapListener listener) {
        this.planTapListener = listener;
    }

    public void setPinDragListener(@Nullable PinDragListener listener) {
        this.pinDragListener = listener;
    }

    public void setDraggablePin(double imgX, double imgY) {
        this.hasDraggablePin = true;
        this.draggablePinX = imgX;
        this.draggablePinY = imgY;
        invalidate();
    }

    public void clearDraggablePin() {
        this.hasDraggablePin = false;
        this.draggablePinX = -1.0;
        this.draggablePinY = -1.0;
        this.activePinDrag = false;
        invalidate();
    }

    public boolean hasDraggablePin() {
        return hasDraggablePin;
    }

    public double getDraggablePinX() {
        return draggablePinX;
    }

    public double getDraggablePinY() {
        return draggablePinY;
    }

    public void setHandles(double h1X, double h1Y, double h2X, double h2Y) {
        this.handle1X = h1X;
        this.handle1Y = h1Y;
        this.handle2X = h2X;
        this.handle2Y = h2Y;
        this.hasHandles = true;
        if (accessibilityHelper != null) {
            accessibilityHelper.invalidateRoot();
        }
        invalidate();
    }

    public void clearHandles() {
        this.hasHandles = false;
        if (accessibilityHelper != null) {
            accessibilityHelper.invalidateRoot();
        }
        invalidate();
    }

    public boolean hasHandles() {
        return hasHandles;
    }

    public double getHandle1X() {
        return handle1X;
    }

    public double getHandle1Y() {
        return handle1Y;
    }

    public double getHandle2X() {
        return handle2X;
    }

    public double getHandle2Y() {
        return handle2Y;
    }

    public ViewportController getViewport() {
        return viewport;
    }

    public double[] imageToView(double[] p) {
        return new double[]{viewport.imageToViewX(p[0]), viewport.imageToViewY(p[1])};
    }

    public double[] viewToImage(double[] p) {
        return new double[]{viewport.viewToImageX(p[0]), viewport.viewToImageY(p[1])};
    }

    public Affine3x3 getImageToViewTransform() {
        return new Affine3x3(viewport.getScale(), 0.0, viewport.getTx(), 0.0, viewport.getScale(), viewport.getTy());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        cancelPendingTileDecode();

        if (scaleDetector != null) {
            scaleDetector.onTouchEvent(event);
        }

        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();

        if (event.getPointerCount() > 1) {
            activeHandle = 0;
            isDragging = false;
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN -> {
                lastTouchX = x;
                lastTouchY = y;
                downTouchX = x;
                downTouchY = y;
                movedBeyondSlop = false;
                activeHandle = 0;
                isDragging = false;

                if (hasHandles) {
                    float hitRadiusPx = 24.0f * getResources().getDisplayMetrics().density;
                    double h1vx = viewport.imageToViewX(handle1X);
                    double h1vy = viewport.imageToViewY(handle1Y);
                    double d1 = Math.hypot(x - h1vx, y - h1vy);

                    double h2vx = viewport.imageToViewX(handle2X);
                    double h2vy = viewport.imageToViewY(handle2Y);
                    double d2 = Math.hypot(x - h2vx, y - h2vy);

                    if (d1 <= hitRadiusPx && d1 <= d2) {
                        activeHandle = 1;
                    } else if (d2 <= hitRadiusPx) {
                        activeHandle = 2;
                    }
                }

                if (hasDraggablePin && activeHandle == 0) {
                    float hitRadiusPx = 36.0f * getResources().getDisplayMetrics().density;
                    double pvx = viewport.imageToViewX(draggablePinX);
                    double pvy = viewport.imageToViewY(draggablePinY);
                    if (Math.hypot(x - pvx, y - pvy) <= hitRadiusPx) {
                        activePinDrag = true;
                        return true;
                    }
                }

                if (activeHandle == 0) {
                    isDragging = true;
                }
                return true;
            }

            case MotionEvent.ACTION_MOVE -> {
                if (activeHandle == 1) {
                    double imgX = viewport.viewToImageX(x);
                    double imgY = viewport.viewToImageY(y);
                    handle1X = Math.max(0.0, Math.min(imageW, imgX));
                    handle1Y = Math.max(0.0, Math.min(imageH, imgY));
                    if (handleMoveListener != null) {
                        handleMoveListener.onHandleMoved(1, handle1X, handle1Y);
                    }
                    invalidate();
                    return true;
                } else if (activeHandle == 2) {
                    double imgX = viewport.viewToImageX(x);
                    double imgY = viewport.viewToImageY(y);
                    handle2X = Math.max(0.0, Math.min(imageW, imgX));
                    handle2Y = Math.max(0.0, Math.min(imageH, imgY));
                    if (handleMoveListener != null) {
                        handleMoveListener.onHandleMoved(2, handle2X, handle2Y);
                    }
                    invalidate();
                    return true;
                } else if (activePinDrag) {
                    double imgX = viewport.viewToImageX(x);
                    double imgY = viewport.viewToImageY(y);
                    draggablePinX = Math.max(0.0, Math.min(imageW, imgX));
                    draggablePinY = Math.max(0.0, Math.min(imageH, imgY));
                    if (pinDragListener != null) {
                        pinDragListener.onPinDragged(draggablePinX, draggablePinY);
                    }
                    invalidate();
                    return true;
                } else if (isDragging) {
                    float dist = (float) Math.hypot(x - downTouchX, y - downTouchY);
                    float touchSlop = 8.0f * getResources().getDisplayMetrics().density;
                    if (dist > touchSlop) {
                        movedBeyondSlop = true;
                    }
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;
                    viewport.pan(dx, dy);
                    lastTouchX = x;
                    lastTouchY = y;
                    invalidate();
                    return true;
                }
            }

            case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activePinDrag) {
                    activePinDrag = false;
                    scheduleGesturesSettled();
                    return true;
                }
                if (action == MotionEvent.ACTION_UP && !movedBeyondSlop && activeHandle == 0) {
                    performClick();
                    if (planTapListener != null) {
                        double imgX = viewport.viewToImageX(x);
                        double imgY = viewport.viewToImageY(y);
                        planTapListener.onPlanTapped(imgX, imgY);
                    }
                }
                activeHandle = 0;
                isDragging = false;
                scheduleGesturesSettled();
                return true;
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void cancelPendingTileDecode() {
        mainHandler.removeCallbacks(settleRunnable);
        if (currentDecodeTask != null) {
            currentDecodeTask.cancel(true);
            currentDecodeTask = null;
        }
        if (tileDecodeListener != null) {
            tileDecodeListener.onTileDecodeCancelled();
        }
    }

    public void scheduleGesturesSettled() {
        mainHandler.removeCallbacks(settleRunnable);
        mainHandler.postDelayed(settleRunnable, 150);
    }

    public void triggerGesturesSettledImmediate() {
        mainHandler.removeCallbacks(settleRunnable);
        onGesturesSettled();
    }

    private void onGesturesSettled() {
        if (imageW <= 0 || imageH <= 0 || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        int screenLong = Math.max(getWidth(), getHeight());
        double[] vp = viewport.getViewportInImage();
        TilePlanner.TilePlan plan = TilePlanner.plan(
                imageW, imageH,
                overviewLong,
                vp[0], vp[1], vp[2], vp[3],
                screenLong
        );

        if (tileDecodeListener != null) {
            tileDecodeListener.onTilePlanComputed(plan);
        }

        if (plan.needsTile()) {
            if (tileDecodeListener != null) {
                tileDecodeListener.onTileDecodeRequested(plan);
            }
            requestTileDecode(plan);
        }
    }

    private void requestTileDecode(TilePlanner.TilePlan plan) {
        if (regionDecoder == null) {
            return;
        }
        currentTilePlan = plan;
        currentDecodeTask = PLAN_EXECUTOR.submit(() -> {
            try {
                Rect rect = new Rect(plan.left(), plan.top(), plan.right(), plan.bottom());
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = plan.sampleSize();
                Bitmap tile = regionDecoder.decodeRegion(rect, opts);
                if (tile != null && !Thread.currentThread().isInterrupted()) {
                    mainHandler.post(() -> {
                        tileBitmap = tile;
                        if (tileDecodeListener != null) {
                            tileDecodeListener.onTileDecoded(tile);
                        }
                        invalidate();
                    });
                }
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (overviewBitmap != null && !overviewBitmap.isRecycled()) {
            drawMatrix.reset();
            double scaleRatioX = (double) imageW / (double) overviewBitmap.getWidth();
            double scaleRatioY = (double) imageH / (double) overviewBitmap.getHeight();
            drawMatrix.postScale(
                    (float) (scaleRatioX * viewport.getScale()),
                    (float) (scaleRatioY * viewport.getScale())
            );
            drawMatrix.postTranslate((float) viewport.getTx(), (float) viewport.getTy());
            canvas.drawBitmap(overviewBitmap, drawMatrix, bitmapPaint);
        }

        if (tileBitmap != null && !tileBitmap.isRecycled() && currentTilePlan != null) {
            Rect src = new Rect(0, 0, tileBitmap.getWidth(), tileBitmap.getHeight());
            RectF dst = new RectF(
                    (float) viewport.imageToViewX(currentTilePlan.left()),
                    (float) viewport.imageToViewY(currentTilePlan.top()),
                    (float) viewport.imageToViewX(currentTilePlan.right()),
                    (float) viewport.imageToViewY(currentTilePlan.bottom())
            );
            canvas.drawBitmap(tileBitmap, src, dst, bitmapPaint);
        }

        Affine3x3 transform = getImageToViewTransform();
        onDrawOverlay(canvas, transform);

        if (hasHandles) {
            drawHandles(canvas);
        }

        if (hasDraggablePin) {
            drawDraggablePin(canvas);
        }
    }

    protected void onDrawOverlay(@NonNull Canvas canvas, @NonNull Affine3x3 imageToView) {
        if (overlayDrawer != null) {
            overlayDrawer.onDrawOverlay(canvas, imageToView);
        }
    }

    private void drawHandles(@NonNull Canvas canvas) {
        float h1x = (float) viewport.imageToViewX(handle1X);
        float h1y = (float) viewport.imageToViewY(handle1Y);
        float h2x = (float) viewport.imageToViewX(handle2X);
        float h2y = (float) viewport.imageToViewY(handle2Y);

        canvas.drawLine(h1x, h1y, h2x, h2y, linePaint);

        float radiusPx = 16.0f * getResources().getDisplayMetrics().density;
        canvas.drawCircle(h1x, h1y, radiusPx, handleInnerPaint);
        canvas.drawCircle(h1x, h1y, radiusPx, handlePaint);

        canvas.drawCircle(h2x, h2y, radiusPx, handleInnerPaint);
        canvas.drawCircle(h2x, h2y, radiusPx, handlePaint);
    }

    private void drawDraggablePin(@NonNull Canvas canvas) {
        float px = (float) viewport.imageToViewX(draggablePinX);
        float py = (float) viewport.imageToViewY(draggablePinY);
        float radius = 16.0f * getResources().getDisplayMetrics().density;
        float innerRadius = 8.0f * getResources().getDisplayMetrics().density;

        handleInnerPaint.setColor(0xFF1976D2);
        canvas.drawCircle(px, py, radius, handleInnerPaint);

        handlePaint.setColor(0xFFFFFFFF);
        canvas.drawCircle(px, py, radius, handlePaint);

        handleInnerPaint.setColor(0xFFFFFFFF);
        canvas.drawCircle(px, py, innerRadius, handleInnerPaint);
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        if (accessibilityHelper != null && accessibilityHelper.dispatchHoverEvent(event)) {
            return true;
        }
        return super.dispatchHoverEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (accessibilityHelper != null && accessibilityHelper.dispatchKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (accessibilityHelper != null) {
            accessibilityHelper.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        }
    }

    private class PlanAccessibilityHelper extends ExploreByTouchHelper {

        PlanAccessibilityHelper(@NonNull View host) {
            super(host);
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            if (!hasHandles) {
                return ExploreByTouchHelper.HOST_ID;
            }
            double h1vx = viewport.imageToViewX(handle1X);
            double h1vy = viewport.imageToViewY(handle1Y);
            double h2vx = viewport.imageToViewX(handle2X);
            double h2vy = viewport.imageToViewY(handle2Y);
            float touchSlop = 48.0f * getResources().getDisplayMetrics().density;
            double d1 = Math.hypot(x - h1vx, y - h1vy);
            double d2 = Math.hypot(x - h2vx, y - h2vy);
            if (d1 <= touchSlop && d1 <= d2) {
                return 1;
            } else if (d2 <= touchSlop) {
                return 2;
            }
            return ExploreByTouchHelper.HOST_ID;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            if (hasHandles) {
                virtualViewIds.add(1);
                virtualViewIds.add(2);
            }
        }

        @Override
        protected void onPopulateNodeForVirtualView(int virtualViewId, @NonNull AccessibilityNodeInfoCompat node) {
            String desc;
            double vx;
            double vy;
            if (virtualViewId == 1) {
                desc = "First point";
                vx = viewport.imageToViewX(handle1X);
                vy = viewport.imageToViewY(handle1Y);
            } else if (virtualViewId == 2) {
                desc = "Second point";
                vx = viewport.imageToViewX(handle2X);
                vy = viewport.imageToViewY(handle2Y);
            } else {
                return;
            }
            node.setContentDescription(desc);
            node.setText(desc);
            node.setFocusable(true);
            node.setClickable(true);
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);

            int halfSize = (int) (24.0f * getResources().getDisplayMetrics().density);
            int ix = (int) Math.round(vx);
            int iy = (int) Math.round(vy);
            Rect bounds = new Rect(ix - halfSize, iy - halfSize, ix + halfSize, iy + halfSize);
            node.setBoundsInParent(bounds);
        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action, @Nullable Bundle arguments) {
            return false;
        }
    }
}
