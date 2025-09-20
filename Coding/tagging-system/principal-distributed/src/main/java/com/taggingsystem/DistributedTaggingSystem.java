package com.taggingsystem;

import com.taggingsystem.cluster.ClusterManager;
import com.taggingsystem.cluster.Node;
import com.taggingsystem.metrics.MetricsCollector;
import com.taggingsystem.partition.PartitionStrategy;
import com.taggingsystem.persistence.PersistenceManager;
import com.taggingsystem.replication.ReplicationManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DistributedTaggingSystem implements AutoCloseable {
    private final ClusterManager clusterManager;
    private final PartitionStrategy partitionStrategy;
    private final ReplicationManager replicationManager;
    private final PersistenceManager persistenceManager;
    private final MetricsCollector metricsCollector;
    private final ExecutorService executorService;
    private final int replicationFactor;

    public DistributedTaggingSystem(Builder builder) {
        this.clusterManager = builder.clusterManager;
        this.partitionStrategy = builder.partitionStrategy;
        this.replicationManager = builder.replicationManager;
        this.persistenceManager = builder.persistenceManager;
        this.metricsCollector = builder.metricsCollector;
        this.replicationFactor = builder.replicationFactor;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public CompletableFuture<Void> tagFileAsync(String filePath, List<String> tags) {
        return CompletableFuture.runAsync(() -> {
            try {
                validateInput(filePath, tags);

                Node primaryNode = partitionStrategy.getPrimaryNode(filePath, clusterManager.getNodes());
                List<Node> replicaNodes = partitionStrategy.getReplicaNodes(filePath,
                    clusterManager.getNodes(), replicationFactor);

                TagOperation operation = new TagOperation(
                    TagOperation.Type.ADD, filePath, tags, System.currentTimeMillis()
                );

                // Execute on primary
                executeOnPrimary(primaryNode, operation);

                // Replicate to replicas asynchronously
                replicateToNodes(replicaNodes, operation);

                // Persist operation
                persistenceManager.persistOperation(operation);

                metricsCollector.recordTagOperation(operation);

            } catch (Exception e) {
                metricsCollector.recordError("tag_file", e);
                throw new TaggingException("Failed to tag file: " + filePath, e);
            }
        }, executorService);
    }

    public CompletableFuture<Void> removeTagFromFileAsync(String filePath, String tag) {
        return CompletableFuture.runAsync(() -> {
            try {
                validateFilePath(filePath);
                validateTag(tag);

                Node primaryNode = partitionStrategy.getPrimaryNode(filePath, clusterManager.getNodes());
                List<Node> replicaNodes = partitionStrategy.getReplicaNodes(filePath,
                    clusterManager.getNodes(), replicationFactor);

                TagOperation operation = new TagOperation(
                    TagOperation.Type.REMOVE, filePath, List.of(tag), System.currentTimeMillis()
                );

                executeOnPrimary(primaryNode, operation);
                replicateToNodes(replicaNodes, operation);
                persistenceManager.persistOperation(operation);
                metricsCollector.recordTagOperation(operation);

            } catch (Exception e) {
                metricsCollector.recordError("remove_tag", e);
                throw new TaggingException("Failed to remove tag from file: " + filePath, e);
            }
        }, executorService);
    }

    public CompletableFuture<List<String>> getFileTagsAsync(String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateFilePath(filePath);

                Node primaryNode = partitionStrategy.getPrimaryNode(filePath, clusterManager.getNodes());
                if (!primaryNode.isAvailable()) {
                    // Fallback to replica
                    List<Node> replicaNodes = partitionStrategy.getReplicaNodes(filePath,
                        clusterManager.getNodes(), replicationFactor);
                    primaryNode = replicaNodes.stream()
                        .filter(Node::isAvailable)
                        .findFirst()
                        .orElseThrow(() -> new TaggingException("No available nodes for file: " + filePath));
                }

                List<String> result = primaryNode.getTaggingSystem().getFileTags(filePath);
                metricsCollector.recordRead();
                return result;

            } catch (Exception e) {
                metricsCollector.recordError("get_file_tags", e);
                throw new TaggingException("Failed to get tags for file: " + filePath, e);
            }
        }, executorService);
    }

    public CompletableFuture<List<String>> getFilesWithTagAsync(String tag) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateTag(tag);

                // Fan-out to all nodes and aggregate results
                List<CompletableFuture<List<String>>> futures = clusterManager.getNodes().stream()
                    .filter(Node::isAvailable)
                    .map(node -> CompletableFuture.supplyAsync(() ->
                        node.getTaggingSystem().getFilesWithTag(tag), executorService))
                    .toList();

                return futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .distinct()
                    .toList();

            } catch (Exception e) {
                metricsCollector.recordError("get_files_with_tag", e);
                throw new TaggingException("Failed to get files with tag: " + tag, e);
            }
        }, executorService);
    }

    public CompletableFuture<List<TagCount>> getTopNTagsAsync(int n) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (n <= 0) {
                    throw new IllegalArgumentException("N must be positive");
                }

                // Fan-out to all nodes and aggregate
                List<CompletableFuture<List<TagCount>>> futures = clusterManager.getNodes().stream()
                    .filter(Node::isAvailable)
                    .map(node -> CompletableFuture.supplyAsync(() ->
                        node.getTaggingSystem().getTopNTags(Integer.MAX_VALUE), executorService))
                    .toList();

                return aggregateTagCounts(futures, n);

            } catch (Exception e) {
                metricsCollector.recordError("get_top_tags", e);
                throw new TaggingException("Failed to get top tags", e);
            }
        }, executorService);
    }

    public CompletableFuture<ClusterHealth> getClusterHealthAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<Node> nodes = clusterManager.getNodes();
            long availableNodes = nodes.stream().mapToLong(node -> node.isAvailable() ? 1 : 0).sum();

            return new ClusterHealth(
                nodes.size(),
                (int) availableNodes,
                metricsCollector.getMetrics()
            );
        }, executorService);
    }

    private void executeOnPrimary(Node primaryNode, TagOperation operation) {
        if (!primaryNode.isAvailable()) {
            throw new TaggingException("Primary node is not available");
        }

        switch (operation.type()) {
            case ADD -> primaryNode.getTaggingSystem().tagFile(operation.filePath(), operation.tags());
            case REMOVE -> operation.tags().forEach(tag ->
                primaryNode.getTaggingSystem().removeTagFromFile(operation.filePath(), tag));
        }
    }

    private void replicateToNodes(List<Node> replicaNodes, TagOperation operation) {
        List<CompletableFuture<Void>> replicationFutures = replicaNodes.stream()
            .filter(Node::isAvailable)
            .map(node -> CompletableFuture.runAsync(() ->
                replicationManager.replicate(node, operation), executorService))
            .toList();

        // Wait for majority of replicas to succeed
        int requiredSuccesses = (replicaNodes.size() / 2) + 1;
        long successCount = replicationFutures.stream()
            .mapToLong(future -> {
                try {
                    future.get(5, TimeUnit.SECONDS);
                    return 1;
                } catch (Exception e) {
                    metricsCollector.recordError("replication", e);
                    return 0;
                }
            }).sum();

        if (successCount < requiredSuccesses) {
            throw new TaggingException("Failed to replicate to majority of nodes");
        }
    }

    private List<TagCount> aggregateTagCounts(List<CompletableFuture<List<TagCount>>> futures, int n) {
        return futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(java.util.stream.Collectors.groupingBy(
                TagCount::tag,
                java.util.stream.Collectors.summingInt(TagCount::count)
            ))
            .entrySet().stream()
            .map(entry -> new TagCount(entry.getKey(), entry.getValue()))
            .sorted((a, b) -> Integer.compare(b.count(), a.count()))
            .limit(n)
            .toList();
    }

    private void validateInput(String filePath, List<String> tags) {
        validateFilePath(filePath);
        if (tags == null || tags.isEmpty()) {
            throw new IllegalArgumentException("Tags cannot be null or empty");
        }
    }

    private void validateFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
    }

    private void validateTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag cannot be null or empty");
        }
    }

    @Override
    public void close() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static class Builder {
        private ClusterManager clusterManager;
        private PartitionStrategy partitionStrategy;
        private ReplicationManager replicationManager;
        private PersistenceManager persistenceManager;
        private MetricsCollector metricsCollector;
        private int replicationFactor = 3;

        public Builder clusterManager(ClusterManager clusterManager) {
            this.clusterManager = clusterManager;
            return this;
        }

        public Builder partitionStrategy(PartitionStrategy partitionStrategy) {
            this.partitionStrategy = partitionStrategy;
            return this;
        }

        public Builder replicationManager(ReplicationManager replicationManager) {
            this.replicationManager = replicationManager;
            return this;
        }

        public Builder persistenceManager(PersistenceManager persistenceManager) {
            this.persistenceManager = persistenceManager;
            return this;
        }

        public Builder metricsCollector(MetricsCollector metricsCollector) {
            this.metricsCollector = metricsCollector;
            return this;
        }

        public Builder replicationFactor(int replicationFactor) {
            this.replicationFactor = replicationFactor;
            return this;
        }

        public DistributedTaggingSystem build() {
            if (clusterManager == null) throw new IllegalStateException("ClusterManager is required");
            if (partitionStrategy == null) throw new IllegalStateException("PartitionStrategy is required");
            if (replicationManager == null) throw new IllegalStateException("ReplicationManager is required");
            if (persistenceManager == null) throw new IllegalStateException("PersistenceManager is required");
            if (metricsCollector == null) throw new IllegalStateException("MetricsCollector is required");

            return new DistributedTaggingSystem(this);
        }
    }

    public record TagCount(String tag, int count) {}
    public record ClusterHealth(int totalNodes, int availableNodes, String metrics) {}
}