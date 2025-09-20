package com.taggingsystem.persistence;

import com.taggingsystem.TagOperation;

import java.util.List;

public interface PersistenceManager {
    void persistOperation(TagOperation operation);
    List<TagOperation> getOperationsSince(long timestamp);
    void compactLog();
    void backup();
    void restore();
}