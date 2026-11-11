package com.netfieldpro.core.verdict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.netfieldpro.core.model.enums.EvidenceGrade;
import com.netfieldpro.core.model.enums.Verdict;

public final class VerdictEngine {

    private VerdictEngine() {
    }

    private static long ceilDiv(long num, long den) {
        if (den <= 0) {
            throw new IllegalArgumentException("den must be > 0");
        }
        if (num <= 0) {
            return 0L;
        }
        return (num - 1) / den + 1;
    }

    private static long floorDiv(long num, long den) {
        if (den <= 0) {
            throw new IllegalArgumentException("den must be > 0");
        }
        if (num <= 0) {
            return 0L;
        }
        return num / den;
    }

    public static Result evaluate(ProfileSpec profile, Inputs inputs) {
        Objects.requireNonNull(profile, "profile cannot be null");
        Objects.requireNonNull(inputs, "inputs cannot be null");

        List<CritResult> rows = new ArrayList<>(CritId.values().length);

        // 1. COVERAGE
        long n = profile.covBasisTapPins() ? inputs.tapPins() : inputs.cellsInside();
        long m = profile.covBasisTapPins() ? inputs.tapPinsMeeting() : inputs.cellsMeeting();
        CritStatus covStatus;
        long covC;
        if (n <= 0) {
            covStatus = CritStatus.NOT_EVALUATED;
            covC = -2L;
        } else {
            covStatus = (m * 100L >= (long) profile.covPct() * n) ? CritStatus.MET : CritStatus.NOT_MET;
            covC = floorDiv(m * 1000L, n);
        }
        rows.add(new CritResult(CritId.COVERAGE, covStatus, covC));

        // 2. SECONDARY
        CritStatus secStatus;
        long secC;
        if (profile.secDbm() == null) {
            secStatus = CritStatus.NOT_REQUIRED;
            secC = -1L;
        } else if (inputs.secCellsMeeting() == null || inputs.cellsInside() <= 0) {
            secStatus = CritStatus.NOT_EVALUATED;
            secC = -2L;
        } else {
            long nSec = inputs.cellsInside();
            long mSec = inputs.secCellsMeeting();
            long secPct = profile.secPct() != null ? profile.secPct() : 0L;
            secStatus = (mSec * 100L >= secPct * nSec) ? CritStatus.MET : CritStatus.NOT_MET;
            secC = floorDiv(mSec * 1000L, nSec);
        }
        rows.add(new CritResult(CritId.SECONDARY, secStatus, secC));

        // 3. P95_RTT
        CritStatus p95Status;
        long p95C;
        if (inputs.p95RttUs() == null) {
            p95Status = CritStatus.NOT_EVALUATED;
            p95C = -2L;
        } else {
            long p95Us = inputs.p95RttUs();
            p95Status = (p95Us <= (long) profile.p95Ms() * 1000L) ? CritStatus.MET : CritStatus.NOT_MET;
            p95C = ceilDiv(p95Us, 100L);
        }
        rows.add(new CritResult(CritId.P95_RTT, p95Status, p95C));

        // 4. MEAN_JITTER
        CritStatus jitStatus;
        long jitC;
        if (inputs.meanJitterMilli() == null) {
            jitStatus = CritStatus.NOT_EVALUATED;
            jitC = -2L;
        } else {
            long milli = inputs.meanJitterMilli();
            jitStatus = (milli <= (long) profile.jitMs() * 1000L) ? CritStatus.MET : CritStatus.NOT_MET;
            jitC = ceilDiv(milli, 100L);
        }
        rows.add(new CritResult(CritId.MEAN_JITTER, jitStatus, jitC));

        // 5. POOLED_LOSS
        CritStatus lossStatus;
        long lossC;
        if (inputs.sentSum() == null || inputs.sentSum() <= 0) {
            lossStatus = CritStatus.NOT_EVALUATED;
            lossC = -2L;
        } else {
            long sent = inputs.sentSum();
            long lost = inputs.lostSum() != null ? inputs.lostSum() : 0L;
            lossStatus = (lost * 1000L <= (long) profile.lossPermille() * sent) ? CritStatus.MET : CritStatus.NOT_MET;
            lossC = ceilDiv(lost * 1000L, sent);
        }
        rows.add(new CritResult(CritId.POOLED_LOSS, lossStatus, lossC));

        // 6. WORST_POINT_LOSS
        CritStatus worstStatus;
        long worstC;
        if (inputs.worstSent() == null || inputs.worstSent() <= 0) {
            worstStatus = CritStatus.NOT_EVALUATED;
            worstC = -2L;
        } else {
            long worstSent = inputs.worstSent();
            long worstLost = inputs.worstLost() != null ? inputs.worstLost() : 0L;
            boolean met;
            if (profile.worstLossInclusive()) {
                met = (worstLost * 100L <= (long) profile.worstLossPct() * worstSent);
            } else {
                met = (worstLost * 100L < (long) profile.worstLossPct() * worstSent);
            }
            worstStatus = met ? CritStatus.MET : CritStatus.NOT_MET;
            worstC = ceilDiv(worstLost * 1000L, worstSent);
        }
        rows.add(new CritResult(CritId.WORST_POINT_LOSS, worstStatus, worstC));

        // 7. STICKY
        CritStatus stickyStatus;
        long stickyC;
        if (profile.sticky() == -1) {
            stickyStatus = CritStatus.NOT_REQUIRED;
            stickyC = -1L;
        } else if (inputs.stickyCount() == null) {
            stickyStatus = CritStatus.NOT_EVALUATED;
            stickyC = -2L;
        } else {
            int count = inputs.stickyCount();
            stickyStatus = (count <= profile.sticky()) ? CritStatus.MET : CritStatus.NOT_MET;
            stickyC = count * 10L;
        }
        rows.add(new CritResult(CritId.STICKY, stickyStatus, stickyC));

        // 8. MASKED
        CritStatus maskedStatus;
        long maskedC;
        if (inputs.cellsInside() <= 0) {
            maskedStatus = CritStatus.MET;
            maskedC = 0L;
        } else {
            long inside = inputs.cellsInside();
            long masked = inputs.cellsMasked();
            maskedC = ceilDiv(masked * 1000L, inside);
            maskedStatus = (masked * 100L <= 5L * inside) ? CritStatus.MET : CritStatus.NOT_EVALUATED;
        }
        rows.add(new CritResult(CritId.MASKED, maskedStatus, maskedC));

        // Verdict precedence
        Verdict verdict;
        if (inputs.grade() == null || inputs.grade() == EvidenceGrade.INDICATIVE) {
            verdict = Verdict.NOT_ELIGIBLE;
        } else {
            boolean hasNotEvaluated = false;
            boolean hasNotMet = false;
            for (CritResult row : rows) {
                if (row.status() == CritStatus.NOT_REQUIRED) {
                    continue;
                }
                if (row.status() == CritStatus.NOT_EVALUATED) {
                    hasNotEvaluated = true;
                } else if (row.status() == CritStatus.NOT_MET) {
                    hasNotMet = true;
                }
            }
            if (hasNotEvaluated) {
                verdict = Verdict.INCOMPLETE;
            } else if (hasNotMet) {
                verdict = Verdict.FAIL;
            } else {
                verdict = Verdict.PASS;
            }
        }

        return new Result(verdict, Collections.unmodifiableList(rows));
    }

    public static long[] cArray(Result result) {
        Objects.requireNonNull(result, "result cannot be null");
        List<CritResult> rows = result.rows();
        long[] c = new long[CritId.values().length];
        for (CritResult row : rows) {
            c[row.id().ordinal()] = row.c();
        }
        return c;
    }
}

