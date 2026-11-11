package com.netfieldpro.core.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class RecoveryPlanner {

    private static final Set<String> SM1_NAMES = Set.of(
            Sm1State.CREATED.name(),
            Sm1State.WARMING_UP.name(),
            Sm1State.WARMUP_FAILED.name(),
            Sm1State.READY.name(),
            Sm1State.ACTIVE.name(),
            Sm1State.ENDED.name()
    );

    public enum Action {
        DELETE,
        SET_WARMUP_FAILED,
        END_REBOOT
    }

    public record SessionFacts(
            String id,
            String stateName,
            long createdAtMs,
            long monoEpochNs,
            int scanCount
    ) {}

    public record PlannedAction(
            String sessionId,
            Action action
    ) {}

    private RecoveryPlanner() {
    }

    public static List<PlannedAction> plan(List<SessionFacts> in, long nowWallMs, long nowMonoNs) {
        if (in == null || in.isEmpty()) {
            return Collections.emptyList();
        }

        List<PlannedAction> result = new ArrayList<>();
        for (SessionFacts facts : in) {
            if (facts == null || facts.stateName() == null) {
                continue;
            }

            // Rule 1: stateName not in the six SM-1 names, or ENDED -> NONE
            if (!SM1_NAMES.contains(facts.stateName()) || Sm1State.ENDED.name().equals(facts.stateName())) {
                continue;
            }

            // Rule 2: CREATED and scanCount == 0 -> DELETE
            if (Sm1State.CREATED.name().equals(facts.stateName()) && facts.scanCount() == 0) {
                result.add(new PlannedAction(facts.id(), Action.DELETE));
                continue;
            }

            // Rule 3: rebooted -> END_REBOOT
            boolean rebooted = RebootDetector.rebooted(facts.createdAtMs(), facts.monoEpochNs(), nowWallMs, nowMonoNs);
            if (rebooted) {
                result.add(new PlannedAction(facts.id(), Action.END_REBOOT));
                continue;
            }

            // Rule 4: WARMING_UP -> SET_WARMUP_FAILED
            if (Sm1State.WARMING_UP.name().equals(facts.stateName())) {
                result.add(new PlannedAction(facts.id(), Action.SET_WARMUP_FAILED));
                continue;
            }

            // Rule 5: else NONE
        }

        return result;
    }
}
