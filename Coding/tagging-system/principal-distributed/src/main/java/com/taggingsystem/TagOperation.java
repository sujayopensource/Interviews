package com.taggingsystem;

import java.util.List;

public record TagOperation(Type type, String filePath, List<String> tags, long timestamp) {
    public enum Type {
        ADD, REMOVE
    }
}