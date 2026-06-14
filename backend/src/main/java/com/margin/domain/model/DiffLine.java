package com.margin.domain.model;

/**
 * A single line within a hunk. {@code newLineNumber} is populated only for
 * ADDED and CONTEXT lines (lines that exist in the new file), and is what the
 * {@code FindingValidator} checks an agent's reported line against.
 */
public record DiffLine(Type type, String content, Integer newLineNumber) {

    public enum Type { ADDED, REMOVED, CONTEXT }

    public boolean isAddressable() {
        return type != Type.REMOVED && newLineNumber != null;
    }
}
