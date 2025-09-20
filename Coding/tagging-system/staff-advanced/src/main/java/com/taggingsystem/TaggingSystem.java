package com.taggingsystem;

import com.taggingsystem.cache.TagCountCache;
import com.taggingsystem.events.TagEvent;
import com.taggingsystem.events.TagEventListener;
import com.taggingsystem.storage.TagStorage;
import com.taggingsystem.storage.InMemoryTagStorage;
import com.taggingsystem.validation.TagValidator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TaggingSystem {
    private final TagStorage storage;
    private final TagCountCache cache;
    private final TagValidator validator;
    private final Set<TagEventListener> listeners;
    private final ReadWriteLock lock;

    public TaggingSystem() {
        this(new InMemoryTagStorage());
    }

    public TaggingSystem(TagStorage storage) {
        this.storage = storage;
        this.cache = new TagCountCache();
        this.validator = new TagValidator();
        this.listeners = ConcurrentHashMap.newKeySet();
        this.lock = new ReentrantReadWriteLock();
    }

    public void tagFile(String filePath, List<String> tags) {
        validator.validateFilePath(filePath);
        validator.validateTags(tags);

        Set<String> normalizedTags = validator.normalizeTags(tags);

        lock.writeLock().lock();
        try {
            Set<String> addedTags = storage.addTagsToFile(filePath, normalizedTags);
            cache.invalidateCache();

            for (String tag : addedTags) {
                notifyListeners(new TagEvent(TagEvent.Type.TAG_ADDED, filePath, tag));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeTagFromFile(String filePath, String tag) {
        validator.validateFilePath(filePath);
        validator.validateTag(tag);

        String normalizedTag = validator.normalizeTag(tag);

        lock.writeLock().lock();
        try {
            boolean removed = storage.removeTagFromFile(filePath, normalizedTag);
            if (removed) {
                cache.invalidateCache();
                notifyListeners(new TagEvent(TagEvent.Type.TAG_REMOVED, filePath, normalizedTag));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<String> getFileTags(String filePath) {
        validator.validateFilePath(filePath);

        lock.readLock().lock();
        try {
            return storage.getFileTags(filePath);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<String> getFilesWithTag(String tag) {
        validator.validateTag(tag);
        String normalizedTag = validator.normalizeTag(tag);

        lock.readLock().lock();
        try {
            return storage.getFilesWithTag(normalizedTag);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<TagCount> getTopNTags(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("N must be positive");
        }

        lock.readLock().lock();
        try {
            return cache.getOrCompute(() -> {
                Map<String, Integer> tagCounts = storage.getTagCounts();
                return tagCounts.entrySet().stream()
                        .map(entry -> new TagCount(entry.getKey(), entry.getValue()))
                        .sorted((a, b) -> Integer.compare(b.count(), a.count()))
                        .limit(n)
                        .toList();
            });
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getTagCount(String tag) {
        validator.validateTag(tag);
        String normalizedTag = validator.normalizeTag(tag);

        lock.readLock().lock();
        try {
            return storage.getTagCount(normalizedTag);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<String> getAllTags() {
        lock.readLock().lock();
        try {
            return storage.getAllTags();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clearFileTags(String filePath) {
        validator.validateFilePath(filePath);

        lock.writeLock().lock();
        try {
            List<String> removedTags = storage.clearFileTags(filePath);
            if (!removedTags.isEmpty()) {
                cache.invalidateCache();
                for (String tag : removedTags) {
                    notifyListeners(new TagEvent(TagEvent.Type.TAG_REMOVED, filePath, tag));
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<String> searchFiles(TagQuery query) {
        lock.readLock().lock();
        try {
            return storage.searchFiles(query);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addListener(TagEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TagEventListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(TagEvent event) {
        for (TagEventListener listener : listeners) {
            try {
                listener.onTagEvent(event);
            } catch (Exception e) {
                // Log error but don't stop other listeners
                System.err.println("Error notifying listener: " + e.getMessage());
            }
        }
    }

    public record TagCount(String tag, int count) {}
}