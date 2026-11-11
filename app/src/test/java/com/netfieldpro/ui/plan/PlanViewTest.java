package com.netfieldpro.ui.plan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.netfieldpro.core.plan.TilePlanner;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PlanViewTest {

    private Context context;
    private PlanView planView;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        planView = new PlanView(context);
        planView.setImageDimensions(2000, 1000);
        planView.measure(
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        );
        planView.layout(0, 0, 1000, 1000);
    }

    private MotionEvent obtainEvent(int action, float x, float y) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        return MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
    }

    @Test
    public void testPV1_pinchRaisesScalePerVP2() {
        // VP-1 initial fit: Image 2000x1000, View 1000x1000 -> fit scale 0.5, tx 0, ty 250
        assertEquals(0.5, planView.getViewport().getScale(), 1e-6);
        assertEquals(0.0, planView.getViewport().getTx(), 1e-6);
        assertEquals(250.0, planView.getViewport().getTy(), 1e-6);

        // VP-2: zoomAt(2.0, (500, 500)) -> scale 1.0, tx -500, ty 0
        planView.getViewport().zoomAt(2.0, 500.0, 500.0);
        assertEquals(1.0, planView.getViewport().getScale(), 1e-6);
        assertEquals(-500.0, planView.getViewport().getTx(), 1e-6);
        assertEquals(0.0, planView.getViewport().getTy(), 1e-6);
    }

    @Test
    public void testPV2_dragPansWithinVP5Clamp() {
        // Set scale to 1.0 as in VP-5
        planView.getViewport().zoomAt(2.0, 500.0, 500.0);
        assertEquals(1.0, planView.getViewport().getScale(), 1e-6);
        assertEquals(-500.0, planView.getViewport().getTx(), 1e-6);
        assertEquals(0.0, planView.getViewport().getTy(), 1e-6);

        // Single finger drag by +10000 in X
        // ACTION_DOWN at (500, 500)
        MotionEvent down = obtainEvent(MotionEvent.ACTION_DOWN, 500.0f, 500.0f);
        planView.onTouchEvent(down);
        down.recycle();

        // ACTION_MOVE to (10500, 500) -> dx = +10000
        MotionEvent moveRight = obtainEvent(MotionEvent.ACTION_MOVE, 10500.0f, 500.0f);
        planView.onTouchEvent(moveRight);
        moveRight.recycle();

        // Clamped to tx = 0.0, ty = 0.0 per VP-5
        assertEquals(0.0, planView.getViewport().getTx(), 1e-6);
        assertEquals(0.0, planView.getViewport().getTy(), 1e-6);

        // Now move left by -20000 (from 10500 to -9500) -> dx = -20000
        MotionEvent moveLeft = obtainEvent(MotionEvent.ACTION_MOVE, -9500.0f, 500.0f);
        planView.onTouchEvent(moveLeft);
        moveLeft.recycle();

        // Clamped to tx = -1000.0, ty = 0.0 per VP-5
        assertEquals(-1000.0, planView.getViewport().getTx(), 1e-6);
        assertEquals(0.0, planView.getViewport().getTy(), 1e-6);

        MotionEvent up = obtainEvent(MotionEvent.ACTION_UP, -9500.0f, 500.0f);
        planView.onTouchEvent(up);
        up.recycle();
    }

    @Test
    public void testPV3_viewToImageImageToViewRoundTrip() {
        // Zoom and pan to arbitrary state
        planView.getViewport().zoomAt(3.5, 400.0, 600.0);
        planView.getViewport().pan(50.0, -30.0);

        double[][] testPoints = new double[][]{
                {0.0, 0.0},
                {100.5, 200.25},
                {999.0, 499.0},
                {1999.9, 999.9},
                {543.21, 876.54}
        };

        for (int i = 0; i < testPoints.length; i++) {
            double[] original = testPoints[i];
            double[] viewCoords = planView.imageToView(original);
            double[] roundTrip = planView.viewToImage(viewCoords);
            assertEquals("Point " + i + " X failed round trip", original[0], roundTrip[0], 1e-6);
            assertEquals("Point " + i + " Y failed round trip", original[1], roundTrip[1], 1e-6);
        }
    }

    @Test
    public void testPV4_handleDragMovesOnlyWithin24dp() {
        // Set handles at (500, 500) and (1500, 500) in image coordinates
        planView.setHandles(500.0, 500.0, 1500.0, 500.0);

        float density = context.getResources().getDisplayMetrics().density;
        float hitRadius = 24.0f * density;

        // In initial fit (scale 0.5, tx 0, ty 250):
        // handle1 image (500, 500) -> view (250, 500)
        double[] h1View = planView.imageToView(new double[]{500.0, 500.0});
        float h1vx = (float) h1View[0];
        float h1vy = (float) h1View[1];

        // Part A: Touch within 24 dp (distance 10 dp < 24 dp)
        float insideTouchX = h1vx + 10.0f * density;
        float insideTouchY = h1vy + 5.0f * density;

        MotionEvent down1 = obtainEvent(MotionEvent.ACTION_DOWN, insideTouchX, insideTouchY);
        planView.onTouchEvent(down1);
        down1.recycle();

        MotionEvent move1 = obtainEvent(MotionEvent.ACTION_MOVE, insideTouchX + 50.0f, insideTouchY + 30.0f);
        planView.onTouchEvent(move1);
        move1.recycle();

        MotionEvent up1 = obtainEvent(MotionEvent.ACTION_UP, insideTouchX + 50.0f, insideTouchY + 30.0f);
        planView.onTouchEvent(up1);
        up1.recycle();

        // Handle 1 should have moved from 500, 500
        double movedX = planView.getHandle1X();
        double movedY = planView.getHandle1Y();
        assertFalse("Handle 1 should have moved", Math.abs(movedX - 500.0) < 1.0);
        assertFalse("Handle 1 should have moved", Math.abs(movedY - 500.0) < 1.0);

        // Reset handle 1 back to 500, 500
        planView.setHandles(500.0, 500.0, 1500.0, 500.0);

        // Part B: Touch OUTSIDE 24 dp (distance 35 dp > 24 dp)
        float outsideTouchX = h1vx + 35.0f * density;
        float outsideTouchY = h1vy + 35.0f * density;

        MotionEvent down2 = obtainEvent(MotionEvent.ACTION_DOWN, outsideTouchX, outsideTouchY);
        planView.onTouchEvent(down2);
        down2.recycle();

        MotionEvent move2 = obtainEvent(MotionEvent.ACTION_MOVE, outsideTouchX + 50.0f, outsideTouchY + 30.0f);
        planView.onTouchEvent(move2);
        move2.recycle();

        MotionEvent up2 = obtainEvent(MotionEvent.ACTION_UP, outsideTouchX + 50.0f, outsideTouchY + 30.0f);
        planView.onTouchEvent(up2);
        up2.recycle();

        // Handle 1 should NOT have moved
        assertEquals(500.0, planView.getHandle1X(), 1e-6);
        assertEquals(500.0, planView.getHandle1Y(), 1e-6);
    }

    @Test
    public void testPV5_tileDecodeRequestedOnlyWhenNeededAndCancelledByGesture() {
        AtomicBoolean decodeRequested = new AtomicBoolean(false);
        AtomicBoolean decodeCancelled = new AtomicBoolean(false);
        AtomicReference<TilePlanner.TilePlan> computedPlan = new AtomicReference<>();

        planView.setTileDecodeListener(new PlanView.TileDecodeListener() {
            @Override
            public void onTilePlanComputed(TilePlanner.TilePlan plan) {
                computedPlan.set(plan);
            }

            @Override
            public void onTileDecodeRequested(TilePlanner.TilePlan plan) {
                decodeRequested.set(true);
            }

            @Override
            public void onTileDecodeCancelled() {
                decodeCancelled.set(true);
            }

            @Override
            public void onTileDecoded(android.graphics.Bitmap tile) {
            }
        });

        // Case 1: Overview mode (scale 0.5, fit scale). TilePlanner TP-1 says no tile needed.
        planView.triggerGesturesSettledImmediate();
        assertNotNull(computedPlan.get());
        assertFalse("At fit scale, needsTile must be false", computedPlan.get().needsTile());
        assertFalse("At fit scale, decode should not be requested", decodeRequested.get());

        // Case 2: Deep zoom (scale 4.0, screen 1000). TilePlanner says tile needed.
        planView.getViewport().zoomAt(8.0, 500.0, 500.0);
        decodeRequested.set(false);
        computedPlan.set(null);

        planView.triggerGesturesSettledImmediate();
        assertNotNull(computedPlan.get());
        assertTrue("At deep zoom, needsTile must be true", computedPlan.get().needsTile());
        assertTrue("At deep zoom, decode should be requested", decodeRequested.get());

        // Case 3: Schedule pending decode and cancel with new gesture
        decodeCancelled.set(false);
        planView.scheduleGesturesSettled();

        // New gesture arrives before 150ms runnable finishes
        MotionEvent down = obtainEvent(MotionEvent.ACTION_DOWN, 500.0f, 500.0f);
        planView.onTouchEvent(down);
        down.recycle();

        assertTrue("New gesture must cancel pending tile decode", decodeCancelled.get());
    }
}
