package me.ph4ntomgg.evolutionsmp.domain;

public enum EvolutionTier {
    T1(1), T2(2), T3(3), T4(4), APEX(5);

    private final int rank;

    EvolutionTier(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public boolean isApex() {
        return this == APEX;
    }
}
