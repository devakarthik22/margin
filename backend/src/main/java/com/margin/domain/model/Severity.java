package com.margin.domain.model;

/**
 * Severity of a finding, ordered by how strongly it should block a merge.
 * The weight encodes the natural ordering used for sorting and verdict logic.
 */
public enum Severity {
    CRITICAL(0),
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private final int weight;

    Severity(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    /** Critical and high findings are considered blocking. */
    public boolean isBlocking() {
        return this == CRITICAL || this == HIGH;
    }
}
