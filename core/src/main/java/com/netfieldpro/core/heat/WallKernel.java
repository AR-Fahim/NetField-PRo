package com.netfieldpro.core.heat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WallKernel {

    private static final double EPS = 1e-9;
    private static final double MERGE_DISTANCE_M = 0.05;
    private static final double NEAR_WALL_THRESHOLD_M = 0.15;

    private static final Map<String, Double> DEFAULT_PENALTIES;
    static {
        Map<String, Double> map = new HashMap<>();
        map.put("DRYWALL", 3.0);
        map.put("DRYWALL_LIGHT", 3.0);
        map.put("GLASS", 2.5);
        map.put("GLASS_PARTITION", 2.5);
        map.put("BRICK", 8.0);
        map.put("BRICK_MEDIUM", 8.0);
        map.put("CONCRETE", 15.0);
        map.put("CONCRETE_HEAVY", 15.0);
        map.put("METAL", 25.0);
        map.put("METAL_ELEVATOR", 25.0);
        DEFAULT_PENALTIES = Collections.unmodifiableMap(map);
    }

    private final List<WallChain> chains;
    private final Map<String, Double> penaltyMap;

    public WallKernel(List<WallChain> chains, Map<String, Double> penaltyM) {
        this.chains = (chains != null) ? Collections.unmodifiableList(new ArrayList<>(chains)) : Collections.emptyList();
        this.penaltyMap = (penaltyM != null) ? new HashMap<>(penaltyM) : new HashMap<>();
    }

    public List<WallChain> chains() {
        return chains;
    }

    public double getPenalty(String materialId) {
        if (materialId == null) {
            return 0.0;
        }
        if (penaltyMap.containsKey(materialId)) {
            Double p = penaltyMap.get(materialId);
            return (p != null) ? p : 0.0;
        }
        String upper = materialId.toUpperCase(Locale.ROOT);
        if (penaltyMap.containsKey(upper)) {
            Double p = penaltyMap.get(upper);
            return (p != null) ? p : 0.0;
        }
        if (DEFAULT_PENALTIES.containsKey(upper)) {
            return DEFAULT_PENALTIES.get(upper);
        }
        return 0.0;
    }

    private static record Crossing(double dCross, double penalty, int chainIndex) {
    }

    public WallHit evaluate(double px, double py, double qx, double qy) {
        // 1. nearWallAtP: distance from P to any wall segment <= 0.15 m
        boolean nearWallAtP = false;
        for (WallChain chain : chains) {
            List<Vertex> vertices = chain.vertices();
            int numVerts = vertices.size();
            for (int i = 0; i < numVerts - 1; i++) {
                Vertex v1 = vertices.get(i);
                Vertex v2 = vertices.get(i + 1);
                double dist = distancePointToSegment(px, py, v1.x(), v1.y(), v2.x(), v2.y());
                if (dist <= NEAR_WALL_THRESHOLD_M) {
                    nearWallAtP = true;
                    break;
                }
            }
            if (nearWallAtP) {
                break;
            }
        }

        double dxPQ = qx - px;
        double dyPQ = qy - py;
        double distPQ = Math.sqrt(dxPQ * dxPQ + dyPQ * dyPQ);
        if (distPQ < EPS) {
            return new WallHit(0, 0.0, nearWallAtP);
        }

        // 2. Crossings
        List<Crossing> rawCrossings = new ArrayList<>();
        for (int chainIdx = 0; chainIdx < chains.size(); chainIdx++) {
            WallChain chain = chains.get(chainIdx);
            double penalty = getPenalty(chain.materialId());
            List<Vertex> vertices = chain.vertices();

            // Sign sequence: orient(P, Q, v). If orient == 0, drop
            List<OrientedVertex> sigmaSeq = new ArrayList<>();
            for (Vertex v : vertices) {
                double orient = dxPQ * (v.y() - py) - dyPQ * (v.x() - px);
                if (orient > 0.0) {
                    sigmaSeq.add(new OrientedVertex(1, v));
                } else if (orient < 0.0) {
                    sigmaSeq.add(new OrientedVertex(-1, v));
                }
            }

            for (int i = 0; i < sigmaSeq.size() - 1; i++) {
                OrientedVertex ov1 = sigmaSeq.get(i);
                OrientedVertex ov2 = sigmaSeq.get(i + 1);
                if (ov1.sign != ov2.sign) {
                    Vertex v1 = ov1.v;
                    Vertex v2 = ov2.v;
                    double orient1 = dxPQ * (v1.y() - py) - dyPQ * (v1.x() - px);
                    double orient2 = dxPQ * (v2.y() - py) - dyPQ * (v2.x() - px);
                    double denom = orient2 - orient1;
                    if (Math.abs(denom) > 1e-15) {
                        double tWall = -orient1 / denom;
                        double crossX = v1.x() + tWall * (v2.x() - v1.x());
                        double crossY = v1.y() + tWall * (v2.y() - v1.y());

                        double tPq = ((crossX - px) * dxPQ + (crossY - py) * dyPQ) / (distPQ * distPQ);
                        if (tPq > EPS && tPq < 1.0 - EPS) {
                            double dCross = tPq * distPQ;
                            rawCrossings.add(new Crossing(dCross, penalty, chainIdx));
                        }
                    }
                }
            }
        }

        if (rawCrossings.isEmpty()) {
            return new WallHit(0, 0.0, nearWallAtP);
        }

        // Sort crossings along PQ
        rawCrossings.sort(Comparator.comparingDouble(Crossing::dCross));

        // Dedupe across DIFFERENT chains within < 0.05m
        List<List<Crossing>> deduped = new ArrayList<>();
        for (Crossing c : rawCrossings) {
            if (deduped.isEmpty()) {
                List<Crossing> group = new ArrayList<>();
                group.add(c);
                deduped.add(group);
            } else {
                List<Crossing> lastGroup = deduped.get(deduped.size() - 1);
                Crossing leader = lastGroup.get(0);
                if ((c.dCross - leader.dCross < MERGE_DISTANCE_M) && (c.chainIndex != leader.chainIndex)) {
                    lastGroup.add(c);
                } else {
                    List<Crossing> group = new ArrayList<>();
                    group.add(c);
                    deduped.add(group);
                }
            }
        }

        double totalPenalty = 0.0;
        for (List<Crossing> group : deduped) {
            double maxP = 0.0;
            for (Crossing c : group) {
                if (c.penalty > maxP) {
                    maxP = c.penalty;
                }
            }
            totalPenalty += maxP;
        }

        return new WallHit(deduped.size(), totalPenalty, nearWallAtP);
    }

    private static double distancePointToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lenSq = dx * dx + dy * dy;
        if (lenSq < 1e-15) {
            double dpx = px - x1;
            double dpy = py - y1;
            return Math.sqrt(dpx * dpx + dpy * dpy);
        }
        double t = ((px - x1) * dx + (py - y1) * dy) / lenSq;
        if (t < 0.0) {
            t = 0.0;
        } else if (t > 1.0) {
            t = 1.0;
        }
        double closeX = x1 + t * dx;
        double closeY = y1 + t * dy;
        double dpx = px - closeX;
        double dpy = py - closeY;
        return Math.sqrt(dpx * dpx + dpy * dpy);
    }

    private static record OrientedVertex(int sign, Vertex v) {
    }
}
