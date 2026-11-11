package com.netfieldpro.core.heat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class IdwEngine {

    public static final int K = 12;
    public static final double COINCIDENT_THRESHOLD = 1e-6;
    public static final double POWER = 2.0;

    private final List<Sample> samples;
    private final WallKernel walls;
    private final double rEff;
    private final Map<Long, List<Sample>> bucketIndex;
    private final boolean useBuckets;

    public IdwEngine(List<Sample> samples, WallKernel walls, double rEff) {
        this.samples = (samples != null) ? Collections.unmodifiableList(new ArrayList<>(samples)) : Collections.emptyList();
        this.walls = walls;
        this.rEff = rEff;

        if (this.rEff > 0.0 && !this.samples.isEmpty()) {
            this.bucketIndex = new HashMap<>();
            this.useBuckets = true;
            for (Sample s : this.samples) {
                int bx = (int) Math.floor(s.x() / this.rEff);
                int by = (int) Math.floor(s.y() / this.rEff);
                long key = (((long) bx) << 32) ^ ((long) by & 0xFFFFFFFFL);
                List<Sample> list = bucketIndex.computeIfAbsent(key, k -> new ArrayList<>());
                list.add(s);
            }
        } else {
            this.bucketIndex = Collections.emptyMap();
            this.useBuckets = false;
        }
    }

    public List<Sample> samples() {
        return samples;
    }

    public WallKernel walls() {
        return walls;
    }

    public double rEff() {
        return rEff;
    }

    private static record Candidate(Sample sample, double dEff) {
    }

    public IdwResult estimate(double cx, double cy) {
        if (samples.isEmpty() || rEff <= 0.0) {
            return new IdwResult(true, Double.NaN, 0);
        }

        List<Sample> pool;
        if (useBuckets) {
            int qbx = (int) Math.floor(cx / rEff);
            int qby = (int) Math.floor(cy / rEff);
            pool = new ArrayList<>();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    long key = (((long) (qbx + dx)) << 32) ^ ((long) (qby + dy) & 0xFFFFFFFFL);
                    List<Sample> inBucket = bucketIndex.get(key);
                    if (inBucket != null) {
                        pool.addAll(inBucket);
                    }
                }
            }
        } else {
            pool = samples;
        }

        // 1. Coincident check (d < 1e-6)
        Sample bestCoincident = null;
        for (Sample s : pool) {
            double dx = s.x() - cx;
            double dy = s.y() - cy;
            double d = Math.sqrt(dx * dx + dy * dy);
            if (d < COINCIDENT_THRESHOLD) {
                if (bestCoincident == null || s.id().compareTo(bestCoincident.id()) < 0) {
                    bestCoincident = s;
                }
            }
        }
        if (bestCoincident != null) {
            return new IdwResult(false, bestCoincident.dbm(), 1);
        }

        // 2 & 3. Candidates: d_eff <= rEff
        List<Candidate> candidates = new ArrayList<>();
        for (Sample s : pool) {
            double dx = s.x() - cx;
            double dy = s.y() - cy;
            double d = Math.sqrt(dx * dx + dy * dy);
            double penalty = (walls != null) ? walls.evaluate(s.x(), s.y(), cx, cy).penaltyM() : 0.0;
            double dEff = d + penalty;
            if (dEff <= rEff) {
                candidates.add(new Candidate(s, dEff));
            }
        }

        if (candidates.isEmpty()) {
            return new IdwResult(true, Double.NaN, 0);
        }

        // 4. Sort: d_eff ascending, then id ascending
        candidates.sort((c1, c2) -> {
            int cmp = Double.compare(c1.dEff, c2.dEff);
            if (cmp != 0) {
                return cmp;
            }
            return c1.sample.id().compareTo(c2.sample.id());
        });

        int k = Math.min(K, candidates.size());
        double num = 0.0;
        double den = 0.0;
        for (int i = 0; i < k; i++) {
            Candidate c = candidates.get(i);
            double w = c.sample.weight() / (c.dEff * c.dEff);
            num += w * c.sample.dbm();
            den += w;
        }

        double dbm = num / den;
        return new IdwResult(false, dbm, k);
    }
}
