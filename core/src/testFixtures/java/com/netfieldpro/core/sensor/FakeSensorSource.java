package com.netfieldpro.core.sensor;

public class FakeSensorSource implements SensorSource {

    private Listener listener;
    private boolean running = false;

    @Override
    public synchronized void start(Listener listener) {
        this.listener = listener;
        this.running = true;
    }

    @Override
    public synchronized void stop() {
        this.running = false;
        this.listener = null;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized void emitSample(long tNs, double x, double y, double z) {
        if (running && listener != null) {
            listener.onSample(tNs, x, y, z);
        }
    }

    public synchronized void emitMagnitude(long tNs, double magnitude) {
        emitSample(tNs, magnitude, 0.0, 0.0);
    }
}
