package com.netfieldpro.core.entitlement;

public enum Tier {
    FREE(0),
    PRO(1),
    ENTERPRISE(2);

    private final int rank;

    Tier(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
