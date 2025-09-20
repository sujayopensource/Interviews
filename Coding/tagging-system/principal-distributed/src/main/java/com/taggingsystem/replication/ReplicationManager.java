package com.taggingsystem.replication;

import com.taggingsystem.TagOperation;
import com.taggingsystem.cluster.Node;

public interface ReplicationManager {
    void replicate(Node node, TagOperation operation);
    void startReplicationLog();
    void stopReplicationLog();
}