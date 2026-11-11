package com.netfieldpro.core.latency;

public record ProbeResult(Status status, long rttUs) {

    public enum Status {
        OK,
        LOST,
        ABORTED
    }

    public static ProbeResult ok(long rttUs) {
        return new ProbeResult(Status.OK, rttUs);
    }

    public static ProbeResult lost() {
        return new ProbeResult(Status.LOST, -1L);
    }

    public static ProbeResult aborted() {
        return new ProbeResult(Status.ABORTED, -1L);
    }

    public boolean isOk() {
        return status == Status.OK;
    }

    public boolean isLost() {
        return status == Status.LOST;
    }

    public boolean isAborted() {
        return status == Status.ABORTED;
    }
}
