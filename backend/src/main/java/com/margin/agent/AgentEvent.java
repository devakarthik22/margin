package com.margin.agent;

/** A single agent lifecycle event emitted while a review runs, streamed to the UI. */
public record AgentEvent(String kind, String text) {
}
