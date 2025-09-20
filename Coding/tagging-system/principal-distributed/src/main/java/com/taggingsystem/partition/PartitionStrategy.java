package com.taggingsystem.partition;

import com.taggingsystem.cluster.Node;

import java.util.List;

public interface PartitionStrategy {
    Node getPrimaryNode(String filePath, List<Node> availableNodes);
    List<Node> getReplicaNodes(String filePath, List<Node> availableNodes, int replicationFactor);
}