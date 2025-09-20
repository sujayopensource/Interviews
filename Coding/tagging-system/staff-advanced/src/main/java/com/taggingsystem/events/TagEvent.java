package com.taggingsystem.events;

import java.time.Instant;

public record TagEvent(Type type, String filePath, String tag, Instant timestamp) {
    public TagEvent(Type type, String filePath, String tag) {
        this(type, filePath, tag, Instant.now());
    }

    public enum Type {
        TAG_ADDED,
        TAG_REMOVED
    }
}