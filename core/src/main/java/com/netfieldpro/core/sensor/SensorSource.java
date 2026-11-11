package com.netfieldpro.core.sensor;

public interface SensorSource {

    interface Listener {
        void onSample(long tNs, double x, double y, double z);
    }

    void start(Listener listener);

    void stop();
}
