package com.netfieldpro.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;

import com.netfieldpro.core.sensor.SensorSource;

import java.util.Objects;

public class AndroidSensorSource implements SensorSource {

    private final Context context;
    private final SensorManager sensorManager;
    private HandlerThread handlerThread;
    private Handler backgroundHandler;
    private SensorEventListener sensorEventListener;
    private boolean running = false;

    public AndroidSensorSource(Context context) {
        this.context = Objects.requireNonNull(context, "context cannot be null").getApplicationContext();
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
    }

    public AndroidSensorSource(Context context, SensorManager sensorManager) {
        this.context = Objects.requireNonNull(context, "context cannot be null").getApplicationContext();
        this.sensorManager = sensorManager;
    }

    @Override
    public synchronized void start(Listener listener) {
        if (running) {
            return;
        }
        Objects.requireNonNull(listener, "listener cannot be null");
        running = true;

        if (sensorManager == null) {
            return;
        }

        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (sensor == null) {
            return;
        }

        handlerThread = new HandlerThread("nf-sensor-thread");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (!running) {
                    return;
                }
                if (event != null && event.values != null && event.values.length >= 3) {
                    listener.onSample(event.timestamp, event.values[0], event.values[1], event.values[2]);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_GAME, backgroundHandler);
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;

        if (sensorManager != null && sensorEventListener != null) {
            sensorManager.unregisterListener(sensorEventListener);
            sensorEventListener = null;
        }

        if (handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
            backgroundHandler = null;
        }
    }

    public synchronized boolean isRunning() {
        return running;
    }
}
