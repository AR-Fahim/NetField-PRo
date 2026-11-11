package com.netfieldpro.core.heat;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public final class HeatPipeline {

    private HeatPipeline() {
    }

    public static record Request(
            Polygon boundary,
            List<WallChain> walls,
            Map<String, Double> wallPenalties,
            List<Sample> samples,
            double rEffMax,
            BooleanSupplier cancelled
    ) {
        public List<WallChain> wallChains() {
            return walls;
        }

        public Request(Polygon boundary, List<WallChain> walls, List<Sample> samples, double rEffMax, BooleanSupplier cancelled) {
            this(boundary, walls, null, samples, rEffMax, cancelled);
        }

        public Request(Polygon boundary, List<WallChain> walls, List<Sample> samples, double rEffMax) {
            this(boundary, walls, null, samples, rEffMax, () -> false);
        }
    }

    public static record Result(
            IdwSurface surface,
            double g,
            int cols,
            int rows,
            int inside,
            int masked,
            long computeMs
    ) {
        public int meeting(double thresholdDbm) {
            if (surface == null) {
                return 0;
            }
            double[] dbm = surface.dbm();
            byte[] cellState = surface.cellState();
            int count = 0;
            for (int i = 0; i < dbm.length; i++) {
                if (cellState[i] == IdwSurface.VALUE && dbm[i] >= thresholdDbm) {
                    count++;
                }
            }
            return count;
        }

        public IdwSurface getSurface() {
            return surface;
        }

        public double getG() {
            return g;
        }

        public int getCols() {
            return cols;
        }

        public int getRows() {
            return rows;
        }

        public int getInside() {
            return inside;
        }

        public int getMasked() {
            return masked;
        }

        public long getComputeMs() {
            return computeMs;
        }
    }

    public static Result compute(Request request, long computeMs) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }
        if (request.boundary() == null) {
            throw new IllegalArgumentException("boundary cannot be null");
        }

        List<Sample> samples = request.samples() != null ? request.samples() : List.of();
        double r = RadiusPolicy.r(samples);
        double rEff = RadiusPolicy.rEff(r, request.rEffMax());

        Grid grid = GridBuilder.build(request.boundary());
        WallKernel wallKernel = new WallKernel(request.walls(), request.wallPenalties());
        IdwEngine engine = new IdwEngine(samples, wallKernel, rEff);

        BooleanSupplier cancelled = request.cancelled() != null ? request.cancelled() : () -> false;
        IdwSurface surface = IdwSurface.compute(grid, request.boundary(), engine, cancelled);

        if (surface == null) {
            return null;
        }

        int inside = surface.masked() + surface.value();
        int masked = surface.masked();

        return new Result(
                surface,
                grid.g(),
                grid.cols(),
                grid.rows(),
                inside,
                masked,
                computeMs
        );
    }

    public static Result compute(Request request) {
        return compute(request, 0L);
    }
}

