package com.netfieldpro.session;

import android.content.Context;

import com.netfieldpro.core.session.RecoveryPlanner;
import com.netfieldpro.core.session.Sm1State;
import com.netfieldpro.core.time.Clock;
import com.netfieldpro.data.repository.SessionRepository;
import com.netfieldpro.time.AndroidClock;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SessionRecovery {

    private static final AtomicBoolean RAN = new AtomicBoolean(false);
    private static Clock clockOverride;
    private static SessionRepository repoOverride;

    private SessionRecovery() {
    }

    public static void resetForTest() {
        RAN.set(false);
        clockOverride = null;
        repoOverride = null;
    }

    public static void setClockForTest(Clock clock) {
        clockOverride = clock;
    }

    public static void setSessionRepositoryForTest(SessionRepository repo) {
        repoOverride = repo;
    }

    public interface Callback {
        void onComplete(boolean anyRebootEnded);
    }

    public static void run(Context context, Callback callback) {
        if (!RAN.compareAndSet(false, true)) {
            if (callback != null) {
                callback.onComplete(false);
            }
            return;
        }

        Clock clock = clockOverride != null ? clockOverride : new AndroidClock();
        SessionRepository repo = repoOverride != null ? repoOverride : SessionRepository.create(context);

        boolean rebootEnded = execute(repo, clock);
        if (callback != null) {
            callback.onComplete(rebootEnded);
        }
    }

    public static boolean execute(SessionRepository repo, Clock clock) {
        List<RecoveryPlanner.SessionFacts> facts = repo.listNonTerminal();
        List<RecoveryPlanner.PlannedAction> plan = RecoveryPlanner.plan(facts, clock.wallMs(), clock.monoNs());

        boolean anyReboot = false;
        for (RecoveryPlanner.PlannedAction pa : plan) {
            switch (pa.action()) {
                case DELETE -> repo.delete(pa.sessionId());
                case SET_WARMUP_FAILED -> repo.updateState(pa.sessionId(), Sm1State.WARMUP_FAILED.name());
                case END_REBOOT -> {
                    repo.updateState(pa.sessionId(), Sm1State.ENDED.name());
                    anyReboot = true;
                }
            }
        }
        return anyReboot;
    }
}
