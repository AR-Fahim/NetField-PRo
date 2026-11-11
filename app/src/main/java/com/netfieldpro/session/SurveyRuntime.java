package com.netfieldpro.session;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.netfieldpro.core.session.EndReason;

public final class SurveyRuntime {

    private static final SurveyRuntime INSTANCE = new SurveyRuntime();

    public enum RuntimeState {
        STOPPED,
        RUNNING,
        BLOCKED
    }

    public record RuntimeStatus(RuntimeState state, String reason) {
        public static RuntimeStatus stopped() {
            return new RuntimeStatus(RuntimeState.STOPPED, null);
        }

        public static RuntimeStatus running() {
            return new RuntimeStatus(RuntimeState.RUNNING, null);
        }

        public static RuntimeStatus blocked(String reason) {
            return new RuntimeStatus(RuntimeState.BLOCKED, reason);
        }
    }

    public enum LockState {
        NONE,
        HELD,
        UNAVAILABLE
    }

    public record LockStatus(LockState state, String reason) {
        public static LockStatus none() {
            return new LockStatus(LockState.NONE, null);
        }

        public static LockStatus held() {
            return new LockStatus(LockState.HELD, null);
        }

        public static LockStatus unavailable(String reason) {
            return new LockStatus(LockState.UNAVAILABLE, reason);
        }
    }

    public interface Listener {
        void onRuntimeChanged(RuntimeStatus status, LockStatus lockStatus);
    }

    private final MutableLiveData<Boolean> keepScreenOn = new MutableLiveData<>(false);
    private volatile RuntimeStatus runtimeStatus = RuntimeStatus.stopped();
    private volatile LockStatus lockStatus = LockStatus.none();
    private volatile EndReason lastEndReason;
    private volatile Listener listener;

    private SurveyRuntime() {
    }

    public static SurveyRuntime get() {
        return INSTANCE;
    }

    public RuntimeStatus getRuntimeStatus() {
        return runtimeStatus;
    }

    public synchronized void setRuntimeStatus(RuntimeStatus status) {
        this.runtimeStatus = status;
        notifyListener();
    }

    public LockStatus getLockStatus() {
        return lockStatus;
    }

    public synchronized void setLockStatus(LockStatus status) {
        this.lockStatus = status;
        notifyListener();
    }

    public LiveData<Boolean> keepScreenOn() {
        return keepScreenOn;
    }

    public MutableLiveData<Boolean> keepScreenOnMutable() {
        return keepScreenOn;
    }

    public EndReason getLastEndReason() {
        return lastEndReason;
    }

    public void setLastEndReason(EndReason reason) {
        this.lastEndReason = reason;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void notifyListener() {
        Listener l = this.listener;
        if (l != null) {
            l.onRuntimeChanged(runtimeStatus, lockStatus);
        }
    }

    public synchronized void resetForTest() {
        runtimeStatus = RuntimeStatus.stopped();
        lockStatus = LockStatus.none();
        lastEndReason = null;
        listener = null;
        keepScreenOn.postValue(false);
    }
}
