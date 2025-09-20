package com.taggingsystem.cache;

import com.taggingsystem.TaggingSystem.TagCount;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class TagCountCache {
    private volatile List<TagCount> cachedTopTags;
    private volatile long lastCacheTime;
    private final long cacheExpirationMs;
    private final ReentrantLock cacheLock;

    public TagCountCache() {
        this(60_000); // 1 minute default expiration
    }

    public TagCountCache(long cacheExpirationMs) {
        this.cacheExpirationMs = cacheExpirationMs;
        this.cacheLock = new ReentrantLock();
    }

    public List<TagCount> getOrCompute(Supplier<List<TagCount>> supplier) {
        long currentTime = System.currentTimeMillis();

        if (cachedTopTags != null && (currentTime - lastCacheTime) < cacheExpirationMs) {
            return cachedTopTags;
        }

        cacheLock.lock();
        try {
            // Double-check pattern
            if (cachedTopTags != null && (currentTime - lastCacheTime) < cacheExpirationMs) {
                return cachedTopTags;
            }

            cachedTopTags = supplier.get();
            lastCacheTime = currentTime;
            return cachedTopTags;
        } finally {
            cacheLock.unlock();
        }
    }

    public void invalidateCache() {
        cacheLock.lock();
        try {
            cachedTopTags = null;
            lastCacheTime = 0;
        } finally {
            cacheLock.unlock();
        }
    }
}