package com.netfieldpro.core.sensor;

import java.util.ArrayList;
import java.util.List;

public final class MotionVariance {

    public static final long WINDOW_NS = 1_000_000_000L;
    public static final double DEFAULT_THRESHOLD = 0.05;

    private long windowStartNs = -1L;
    private final List<Double> currentWindow = new ArrayList<>();
    private double maxVariance = 0.0;
    private int closedWindowCount = 0;

    public MotionVariance() {
    }

    public synchronized void addSample(long tNs, double x, double y, double z) {
        double magnitude = Math.sqrt(x * x + y * y + z * z);
        addSample(tNs, magnitude);
    }

    public synchronized void addMagnitude(long tNs, double magnitude) {
        addSample(tNs, magnitude);
    }

    public synchronized void addSample(long tNs, double magnitude) {
        if (windowStartNs < 0) {
            windowStartNs = tNs;
            currentWindow.add(magnitude);
        } else if (tNs - windowStartNs < WINDOW_NS) {
            currentWindow.add(magnitude);
        } else {
            closeActiveWindow();
            windowStartNs = tNs;
            currentWindow.add(magnitude);
        }
    }

    public synchronized void closeWindow() {
        closeActiveWindow();
        windowStartNs = -1L;
    }

    private void closeActiveWindow() {
        if (!currentWindow.isEmpty()) {
            double var = computeVariance(currentWindow);
            maxVariance = Math.max(maxVariance, var);
            closedWindowCount++;
            currentWindow.clear();
        }
    }

    private double computeVariance(List<Double> samples) {
        int n = samples.size();
        if (n <= 1) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : samples) {
            sum += v;
        }
        double mean = sum / n;
        double sumSqDiff = 0.0;
        for (double v : samples) {
            double diff = v - mean;
            sumSqDiff += diff * diff;
        }
        return sumSqDiff / n;
    }

    public synchronized double maxVariance() {
        if (closedWindowCount == 0 && !currentWindow.isEmpty()) {
            double activeVar = computeVariance(currentWindow);
            return Math.max(maxVariance, activeVar);
        }
        return maxVariance;
    }

    public synchronized boolean moved() {
        return moved(DEFAULT_THRESHOLD);
    }

    public synchronized boolean moved(double threshold) {
        return maxVariance() > threshold;
    }

    public synchronized int getClosedWindowCount() {
        return closedWindowCount;
    }

    public synchronized void reset() {
        windowStartNs = -1L;
        currentWindow.clear();
        maxVariance = 0.0;
        closedWindowCount = 0;
    }
}
