package com.taggingsystem.cluster;

import com.taggingsystem.TaggingSystem;

public interface Node {
    String getId();
    String getAddress();
    boolean isAvailable();
    void markUnavailable();
    void markAvailable();
    TaggingSystem getTaggingSystem();
    long getLastHeartbeat();
    void updateHeartbeat();
}