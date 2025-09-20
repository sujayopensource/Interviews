package com.taggingsystem;

import com.taggingsystem.cluster.ClusterManager;
import com.taggingsystem.cluster.Node;
import com.taggingsystem.metrics.MetricsCollector;
import com.taggingsystem.partition.PartitionStrategy;
import com.taggingsystem.persistence.PersistenceManager;
import com.taggingsystem.replication.ReplicationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Principal Distributed Tagging System Tests")
class DistributedTaggingSystemTest {

    @Mock private ClusterManager clusterManager;
    @Mock private PartitionStrategy partitionStrategy;
    @Mock private ReplicationManager replicationManager;
    @Mock private PersistenceManager persistenceManager;
    @Mock private MetricsCollector metricsCollector;
    @Mock private Node primaryNode;
    @Mock private Node replicaNode1;
    @Mock private Node replicaNode2;
    @Mock private TaggingSystem nodeTaggingSystem;

    private DistributedTaggingSystem distributedTaggingSystem;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(clusterManager.getNodes()).thenReturn(List.of(primaryNode, replicaNode1, replicaNode2));
        when(partitionStrategy.getPrimaryNode(anyString(), any())).thenReturn(primaryNode);
        when(partitionStrategy.getReplicaNodes(anyString(), any(), anyInt()))
            .thenReturn(List.of(replicaNode1, replicaNode2));

        when(primaryNode.isAvailable()).thenReturn(true);
        when(replicaNode1.isAvailable()).thenReturn(true);
        when(replicaNode2.isAvailable()).thenReturn(true);
        when(primaryNode.getTaggingSystem()).thenReturn(nodeTaggingSystem);
        when(replicaNode1.getTaggingSystem()).thenReturn(nodeTaggingSystem);
        when(replicaNode2.getTaggingSystem()).thenReturn(nodeTaggingSystem);

        distributedTaggingSystem = new DistributedTaggingSystem.Builder()
            .clusterManager(clusterManager)
            .partitionStrategy(partitionStrategy)
            .replicationManager(replicationManager)
            .persistenceManager(persistenceManager)
            .metricsCollector(metricsCollector)
            .replicationFactor(2)
            .build();
    }

    @Test
    @DisplayName("Should tag file asynchronously across cluster")
    void shouldTagFileAsynchronouslyAcrossCluster() throws Exception {
        String filePath = "/distributed/file.txt";
        List<String> tags = List.of("distributed", "system");

        CompletableFuture<Void> future = distributedTaggingSystem.tagFileAsync(filePath, tags);
        future.get(10, TimeUnit.SECONDS);

        verify(nodeTaggingSystem).tagFile(filePath, tags);
        verify(replicationManager, times(2)).replicate(any(Node.class), any(TagOperation.class));
        verify(persistenceManager).persistOperation(any(TagOperation.class));
        verify(metricsCollector).recordTagOperation(any(TagOperation.class));
    }

    @Test
    @DisplayName("Should handle primary node failure gracefully")
    void shouldHandlePrimaryNodeFailureGracefully() throws Exception {
        String filePath = "/test/file.txt";
        when(primaryNode.isAvailable()).thenReturn(false);
        when(replicaNode1.isAvailable()).thenReturn(true);
        when(nodeTaggingSystem.getFileTags(filePath)).thenReturn(List.of("tag1", "tag2"));

        CompletableFuture<List<String>> future = distributedTaggingSystem.getFileTagsAsync(filePath);
        List<String> result = future.get(10, TimeUnit.SECONDS);

        assertEquals(2, result.size());
        verify(metricsCollector).recordRead();
    }

    @Test
    @DisplayName("Should aggregate top tags across all nodes")
    void shouldAggregateTopTagsAcrossAllNodes() throws Exception {
        when(nodeTaggingSystem.getTopNTags(Integer.MAX_VALUE))
            .thenReturn(List.of(
                new DistributedTaggingSystem.TagCount("java", 5),
                new DistributedTaggingSystem.TagCount("python", 3)
            ))
            .thenReturn(List.of(
                new DistributedTaggingSystem.TagCount("java", 3),
                new DistributedTaggingSystem.TagCount("scala", 2)
            ))
            .thenReturn(List.of(
                new DistributedTaggingSystem.TagCount("python", 4),
                new DistributedTaggingSystem.TagCount("go", 1)
            ));

        CompletableFuture<List<DistributedTaggingSystem.TagCount>> future =
            distributedTaggingSystem.getTopNTagsAsync(3);
        List<DistributedTaggingSystem.TagCount> result = future.get(10, TimeUnit.SECONDS);

        assertEquals(3, result.size());
        assertEquals("java", result.get(0).tag());
        assertEquals(8, result.get(0).count()); // 5 + 3
        assertEquals("python", result.get(1).tag());
        assertEquals(7, result.get(1).count()); // 3 + 4
        assertEquals("scala", result.get(2).tag());
        assertEquals(2, result.get(2).count());
    }

    @Test
    @DisplayName("Should handle replication failures")
    void shouldHandleReplicationFailures() {
        String filePath = "/test/file.txt";
        List<String> tags = List.of("test");

        // Make one replica unavailable
        when(replicaNode2.isAvailable()).thenReturn(false);
        doThrow(new RuntimeException("Replication failed"))
            .when(replicationManager).replicate(eq(replicaNode1), any(TagOperation.class));

        assertThrows(TaggingException.class, () -> {
            try {
                distributedTaggingSystem.tagFileAsync(filePath, tags).get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                if (e.getCause() instanceof TaggingException) {
                    throw (TaggingException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("Should provide cluster health information")
    void shouldProvideClusterHealthInformation() throws Exception {
        when(metricsCollector.getMetrics()).thenReturn("metrics-data");

        CompletableFuture<DistributedTaggingSystem.ClusterHealth> future =
            distributedTaggingSystem.getClusterHealthAsync();
        DistributedTaggingSystem.ClusterHealth health = future.get(10, TimeUnit.SECONDS);

        assertEquals(3, health.totalNodes());
        assertEquals(3, health.availableNodes());
        assertEquals("metrics-data", health.metrics());
    }

    @Test
    @DisplayName("Should handle concurrent operations safely")
    void shouldHandleConcurrentOperationsSafely() throws Exception {
        List<CompletableFuture<Void>> futures = List.of(
            distributedTaggingSystem.tagFileAsync("/file1.txt", List.of("concurrent1")),
            distributedTaggingSystem.tagFileAsync("/file2.txt", List.of("concurrent2")),
            distributedTaggingSystem.tagFileAsync("/file3.txt", List.of("concurrent3"))
        );

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .get(15, TimeUnit.SECONDS);

        verify(nodeTaggingSystem, times(3)).tagFile(anyString(), anyList());
        verify(replicationManager, times(6)).replicate(any(Node.class), any(TagOperation.class));
    }

    @Test
    @DisplayName("Should validate input parameters")
    void shouldValidateInputParameters() {
        assertThrows(IllegalArgumentException.class, () ->
            distributedTaggingSystem.tagFileAsync(null, List.of("tag")));

        assertThrows(IllegalArgumentException.class, () ->
            distributedTaggingSystem.tagFileAsync("/file.txt", null));

        assertThrows(IllegalArgumentException.class, () ->
            distributedTaggingSystem.getTopNTagsAsync(-1));
    }

    @Test
    @DisplayName("Should build system with required components")
    void shouldBuildSystemWithRequiredComponents() {
        assertThrows(IllegalStateException.class, () ->
            new DistributedTaggingSystem.Builder().build());

        assertThrows(IllegalStateException.class, () ->
            new DistributedTaggingSystem.Builder()
                .clusterManager(clusterManager)
                .build());
    }
}