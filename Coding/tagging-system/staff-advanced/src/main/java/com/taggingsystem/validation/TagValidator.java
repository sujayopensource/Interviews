package com.taggingsystem.validation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TagValidator {
    private static final int MAX_TAG_LENGTH = 100;
    private static final String INVALID_CHARACTERS = "[^a-zA-Z0-9_-]";

    public void validateFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
    }

    public void validateTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            throw new IllegalArgumentException("Tags list cannot be null or empty");
        }
    }

    public void validateTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag cannot be null or empty");
        }
    }

    public Set<String> normalizeTags(List<String> tags) {
        return tags.stream()
                .map(this::normalizeTag)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toSet());
    }

    public String normalizeTag(String tag) {
        String normalized = tag.trim().toLowerCase();

        if (normalized.length() > MAX_TAG_LENGTH) {
            throw new IllegalArgumentException("Tag length cannot exceed " + MAX_TAG_LENGTH + " characters");
        }

        if (normalized.matches(".*" + INVALID_CHARACTERS + ".*")) {
            throw new IllegalArgumentException("Tag contains invalid characters. Only alphanumeric, underscore, and hyphen are allowed");
        }

        return normalized;
    }
}