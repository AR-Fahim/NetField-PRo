package com.netfieldpro.session;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.netfieldpro.core.session.Sm1State;
import com.netfieldpro.data.repository.SessionRepository;

import java.util.Objects;
import java.util.function.Supplier;

public class DefaultEffectSink implements EffectSink {

    private final Context context;
    private final SessionRepository repository;
    private final Supplier<String> currentSessionIdSupplier;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingIdleRunnable;

    public DefaultEffectSink(Context context, SessionRepository repository, Supplier<String> currentSessionIdSupplier) {
        this.context = Objects.requireNonNull(context, "context cannot be null").getApplicationContext();
        this.repository = repository;
        this.currentSessionIdSupplier = currentSessionIdSupplier;
    }

    @Override
    public void onPersistState(Sm1State state) throws Exception {
        if (repository != null && currentSessionIdSupplier != null) {
            String sessionId = currentSessionIdSupplier.get();
            if (sessionId != null) {
                repository.updateState(sessionId, state.name());
            }
        }
    }

    @Override
    public void onRuntimeStart() throws Exception {
        Intent intent = new Intent(context, SurveyForegroundService.class);
        intent.setAction(SurveyForegroundService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Override
    public void onRuntimeStop() throws Exception {
        Intent intent = new Intent(context, SurveyForegroundService.class);
        context.stopService(intent);
    }

    @Override
    public void onScreenFlag(boolean keepScreenOn) throws Exception {
        SurveyRuntime.get().keepScreenOnMutable().postValue(keepScreenOn);
    }

    @Override
    public void onScheduleIdle(long atNs, Runnable tickRunnable, long delayMs) throws Exception {
        onCancelIdle();
        this.pendingIdleRunnable = tickRunnable;
        handler.postDelayed(tickRunnable, delayMs);
    }

    @Override
    public void onCancelIdle() throws Exception {
        if (pendingIdleRunnable != null) {
            handler.removeCallbacks(pendingIdleRunnable);
            pendingIdleRunnable = null;
        }
    }

    @Override
    public void onLogGap(String gapKind, long atNs) throws Exception {
        if (repository != null && currentSessionIdSupplier != null) {
            String sessionId = currentSessionIdSupplier.get();
            if (sessionId != null) {
                repository.appendGap(sessionId, gapKind, atNs);
            }
        }
    }

    @Override
    public void onMarkIndicative() throws Exception {
        if (repository != null && currentSessionIdSupplier != null) {
            String sessionId = currentSessionIdSupplier.get();
            if (sessionId != null) {
                repository.setEvidenceGrade(sessionId, "INDICATIVE");
            }
        }
    }
}
