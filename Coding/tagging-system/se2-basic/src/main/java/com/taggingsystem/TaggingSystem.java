package com.taggingsystem;

import java.util.*;
import java.util.stream.Collectors;

public class TaggingSystem {
    private final Map<String, Set<String>> fileToTags;
    private final Map<String, Set<String>> tagToFiles;

    public TaggingSystem() {
        this.fileToTags = new HashMap<>();
        this.tagToFiles = new HashMap<>();
    }

    public void tagFile(String filePath, List<String> tags) {
        validateFilePath(filePath);
        validateTags(tags);

        Set<String> normalizedTags = tags.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toSet());

        fileToTags.computeIfAbsent(filePath, k -> new HashSet<>()).addAll(normalizedTags);

        for (String tag : normalizedTags) {
            tagToFiles.computeIfAbsent(tag, k -> new HashSet<>()).add(filePath);
        }
    }

    public void removeTagFromFile(String filePath, String tag) {
        validateFilePath(filePath);
        validateTag(tag);

        String normalizedTag = tag.trim().toLowerCase();

        Set<String> fileTags = fileToTags.get(filePath);
        if (fileTags != null) {
            fileTags.remove(normalizedTag);
            if (fileTags.isEmpty()) {
                fileToTags.remove(filePath);
            }
        }

        Set<String> tagFiles = tagToFiles.get(normalizedTag);
        if (tagFiles != null) {
            tagFiles.remove(filePath);
            if (tagFiles.isEmpty()) {
                tagToFiles.remove(normalizedTag);
            }
        }
    }

    public List<String> getFileTags(String filePath) {
        validateFilePath(filePath);
        return new ArrayList<>(fileToTags.getOrDefault(filePath, Collections.emptySet()));
    }

    public List<String> getFilesWithTag(String tag) {
        validateTag(tag);
        String normalizedTag = tag.trim().toLowerCase();
        return new ArrayList<>(tagToFiles.getOrDefault(normalizedTag, Collections.emptySet()));
    }

    public List<TagCount> getTopNTags(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("N must be positive");
        }

        return tagToFiles.entrySet().stream()
                .map(entry -> new TagCount(entry.getKey(), entry.getValue().size()))
                .sorted((a, b) -> Integer.compare(b.count(), a.count()))
                .limit(n)
                .collect(Collectors.toList());
    }

    public int getTagCount(String tag) {
        validateTag(tag);
        String normalizedTag = tag.trim().toLowerCase();
        return tagToFiles.getOrDefault(normalizedTag, Collections.emptySet()).size();
    }

    public List<String> getAllTags() {
        return new ArrayList<>(tagToFiles.keySet());
    }

    public void clearFileTags(String filePath) {
        validateFilePath(filePath);

        Set<String> tags = fileToTags.remove(filePath);
        if (tags != null) {
            for (String tag : tags) {
                Set<String> files = tagToFiles.get(tag);
                if (files != null) {
                    files.remove(filePath);
                    if (files.isEmpty()) {
                        tagToFiles.remove(tag);
                    }
                }
            }
        }
    }

    private void validateFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
    }

    private void validateTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            throw new IllegalArgumentException("Tags list cannot be null or empty");
        }
    }

    private void validateTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag cannot be null or empty");
        }
    }

    public record TagCount(String tag, int count) {}
}