package com.taggingsystem.storage;

import com.taggingsystem.TagQuery;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryTagStorage implements TagStorage {
    private final Map<String, Set<String>> fileToTags = new HashMap<>();
    private final Map<String, Set<String>> tagToFiles = new HashMap<>();

    @Override
    public Set<String> addTagsToFile(String filePath, Set<String> tags) {
        Set<String> existingTags = fileToTags.computeIfAbsent(filePath, k -> new HashSet<>());
        Set<String> newTags = new HashSet<>();

        for (String tag : tags) {
            if (existingTags.add(tag)) {
                tagToFiles.computeIfAbsent(tag, k -> new HashSet<>()).add(filePath);
                newTags.add(tag);
            }
        }

        return newTags;
    }

    @Override
    public boolean removeTagFromFile(String filePath, String tag) {
        Set<String> fileTags = fileToTags.get(filePath);
        if (fileTags == null || !fileTags.remove(tag)) {
            return false;
        }

        if (fileTags.isEmpty()) {
            fileToTags.remove(filePath);
        }

        Set<String> tagFiles = tagToFiles.get(tag);
        if (tagFiles != null) {
            tagFiles.remove(filePath);
            if (tagFiles.isEmpty()) {
                tagToFiles.remove(tag);
            }
        }

        return true;
    }

    @Override
    public List<String> getFileTags(String filePath) {
        return new ArrayList<>(fileToTags.getOrDefault(filePath, Collections.emptySet()));
    }

    @Override
    public List<String> getFilesWithTag(String tag) {
        return new ArrayList<>(tagToFiles.getOrDefault(tag, Collections.emptySet()));
    }

    @Override
    public Map<String, Integer> getTagCounts() {
        return tagToFiles.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                ));
    }

    @Override
    public int getTagCount(String tag) {
        return tagToFiles.getOrDefault(tag, Collections.emptySet()).size();
    }

    @Override
    public List<String> getAllTags() {
        return new ArrayList<>(tagToFiles.keySet());
    }

    @Override
    public List<String> clearFileTags(String filePath) {
        Set<String> removedTags = fileToTags.remove(filePath);
        if (removedTags == null) {
            return Collections.emptyList();
        }

        for (String tag : removedTags) {
            Set<String> files = tagToFiles.get(tag);
            if (files != null) {
                files.remove(filePath);
                if (files.isEmpty()) {
                    tagToFiles.remove(tag);
                }
            }
        }

        return new ArrayList<>(removedTags);
    }

    @Override
    public List<String> searchFiles(TagQuery query) {
        Set<String> result = new HashSet<>();
        boolean firstOperation = true;

        for (String tag : query.getRequiredTags()) {
            Set<String> filesWithTag = tagToFiles.getOrDefault(tag, Collections.emptySet());
            if (firstOperation) {
                result.addAll(filesWithTag);
                firstOperation = false;
            } else {
                result.retainAll(filesWithTag);
            }
        }

        for (String tag : query.getOptionalTags()) {
            Set<String> filesWithTag = tagToFiles.getOrDefault(tag, Collections.emptySet());
            result.addAll(filesWithTag);
        }

        for (String tag : query.getExcludedTags()) {
            Set<String> filesWithTag = tagToFiles.getOrDefault(tag, Collections.emptySet());
            result.removeAll(filesWithTag);
        }

        return new ArrayList<>(result);
    }
}