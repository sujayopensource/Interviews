package com.taggingsystem.cluster;

import java.util.List;

public interface ClusterManager {
    List<Node> getNodes();
    void addNode(Node node);
    void removeNode(String nodeId);
    void handleNodeFailure(String nodeId);
    void startHealthCheck();
    void stopHealthCheck();
}