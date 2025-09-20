package com.taggingsystem.partition;

import com.taggingsystem.cluster.Node;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class ConsistentHashingStrategy implements PartitionStrategy {
    private final TreeMap<Long, Node> ring;
    private final int virtualNodes;

    public ConsistentHashingStrategy(int virtualNodes) {
        this.ring = new TreeMap<>();
        this.virtualNodes = virtualNodes;
    }

    public void addNode(Node node) {
        for (int i = 0; i < virtualNodes; i++) {
            long hash = hash(node.getId() + ":" + i);
            ring.put(hash, node);
        }
    }

    public void removeNode(Node node) {
        for (int i = 0; i < virtualNodes; i++) {
            long hash = hash(node.getId() + ":" + i);
            ring.remove(hash);
        }
    }

    @Override
    public Node getPrimaryNode(String filePath, List<Node> availableNodes) {
        if (availableNodes.isEmpty()) {
            throw new IllegalArgumentException("No available nodes");
        }

        updateRing(availableNodes);
        long hash = hash(filePath);

        Map.Entry<Long, Node> entry = ring.higherEntry(hash);
        if (entry == null) {
            entry = ring.firstEntry();
        }

        return entry.getValue();
    }

    @Override
    public List<Node> getReplicaNodes(String filePath, List<Node> availableNodes, int replicationFactor) {
        if (availableNodes.size() <= 1) {
            return List.of();
        }

        updateRing(availableNodes);
        long hash = hash(filePath);

        Set<Node> replicas = new LinkedHashSet<>();
        NavigableMap<Long, Node> tailMap = ring.tailMap(hash, false);
        NavigableMap<Long, Node> headMap = ring.headMap(hash, true);

        // Add nodes after the hash
        for (Node node : tailMap.values()) {
            if (replicas.size() >= replicationFactor) break;
            replicas.add(node);
        }

        // Wrap around if needed
        for (Node node : headMap.values()) {
            if (replicas.size() >= replicationFactor) break;
            replicas.add(node);
        }

        return new ArrayList<>(replicas);
    }

    private void updateRing(List<Node> availableNodes) {
        Set<Node> currentNodes = new HashSet<>(ring.values());
        Set<Node> newNodes = new HashSet<>(availableNodes);

        // Remove nodes that are no longer available
        for (Node node : currentNodes) {
            if (!newNodes.contains(node)) {
                removeNode(node);
            }
        }

        // Add new nodes
        for (Node node : newNodes) {
            if (!currentNodes.contains(node)) {
                addNode(node);
            }
        }
    }

    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
}