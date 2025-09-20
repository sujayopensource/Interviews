package com.taggingsystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Advanced Tagging System - Principal Level Tests")
class AdvancedTaggingSystemTest {

    private AdvancedTaggingSystem system;

    @BeforeEach
    void setUp() {
        system = new AdvancedTaggingSystem(1000, 10000);
    }

    @Test
    @DisplayName("Should demonstrate O(1) amortized top-K performance")
    void shouldDemonstrateTopKPerformance() {
        // Load system with data
        for (int i = 0; i < 1000; i++) {
            system.tagFile("/file" + i, List.of("tag" + (i % 100), "common", "test"));
        }

        long start = System.nanoTime();

        // Multiple top-K queries should be very fast due to optimization
        for (int i = 0; i < 100; i++) {
            List<AdvancedTaggingSystem.TagCount> topTags = system.getTopKTags(10);
            assertFalse(topTags.isEmpty());
        }

        long duration = System.nanoTime() - start;
        System.out.println("100 top-K queries took: " + duration / 1_000_000 + "ms");

        // Should be very fast due to optimized tracking
        assertTrue(duration < 50_000_000, "Top-K queries should be under 50ms total");
    }

    @Test
    @DisplayName("Should handle complex boolean queries efficiently")
    void shouldHandleComplexBooleanQueries() {
        // Setup test data
        system.tagFile("/java-basics.pdf", List.of("java", "programming", "beginner", "tutorial"));
        system.tagFile("/java-advanced.pdf", List.of("java", "programming", "advanced", "expert"));
        system.tagFile("/python-basics.py", List.of("python", "programming", "beginner", "tutorial"));
        system.tagFile("/scala-guide.md", List.of("scala", "programming", "functional", "advanced"));
        system.tagFile("/design-patterns.pdf", List.of("java", "design", "patterns", "advanced"));

        // Complex query: (java OR python) AND programming AND advanced BUT NOT tutorial
        AdvancedTaggingSystem.ComplexQuery query = new AdvancedTaggingSystem.ComplexQuery(
            Set.of("programming", "advanced"),  // required (AND)
            Set.of("java", "python"),           // optional (OR)
            Set.of("tutorial")                  // excluded (NOT)
        );

        List<String> results = system.searchFiles(query);

        assertEquals(2, results.size());
        assertTrue(results.contains("/java-advanced.pdf"));
        assertTrue(results.contains("/design-patterns.pdf"));
        assertFalse(results.contains("/java-basics.pdf")); // excluded due to tutorial
        assertFalse(results.contains("/python-basics.py")); // excluded due to tutorial
    }

    @Test
    @DisplayName("Should demonstrate Bloom filter effectiveness")
    void shouldDemonstrateBloomFilterEffectiveness() {
        // Add many tags to populate bloom filter
        for (int i = 0; i < 1000; i++) {
            system.tagFile("/file" + i, List.of("tag" + i, "common"));
        }

        // Test bloom filter accuracy
        int falsePositives = 0;
        int trueNegatives = 0;

        for (int i = 1000; i < 2000; i++) {
            String nonExistentTag = "tag" + i;
            int frequency = system.getTagFrequency(nonExistentTag);

            if (frequency == 0) {
                trueNegatives++;
            } else {
                falsePositives++;
            }
        }

        // Bloom filter should have very low false positive rate
        double falsePositiveRate = (double) falsePositives / (falsePositives + trueNegatives);
        System.out.println("Bloom filter false positive rate: " + falsePositiveRate);
        assertTrue(falsePositiveRate < 0.05, "False positive rate should be under 5%");
    }

    @Test
    @DisplayName("Should handle high-concurrency scenarios with advanced locking")
    @Timeout(30)
    void shouldHandleHighConcurrencyWithAdvancedLocking() throws InterruptedException {
        int numThreads = 50;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Mixed workload: reads and writes
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        if (i % 3 == 0) {
                            // Write operation
                            system.tagFile("/file" + threadId + "_" + i,
                                         List.of("thread" + threadId, "iteration" + i, "concurrent"));
                        } else {
                            // Read operation
                            system.getTopKTags(10);
                            system.getTagFrequency("concurrent");
                        }
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(25, TimeUnit.SECONDS));
        executor.shutdown();

        // Verify no exceptions occurred
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent operations");

        // Verify data consistency
        List<AdvancedTaggingSystem.TagCount> topTags = system.getTopKTags(5);
        assertFalse(topTags.isEmpty());

        // "concurrent" tag should appear many times
        int concurrentCount = system.getTagFrequency("concurrent");
        assertTrue(concurrentCount > numThreads * operationsPerThread / 4,
                  "Concurrent tag should appear frequently");
    }

    @Test
    @DisplayName("Should demonstrate LRU cache effectiveness")
    void shouldDemonstrateLRUCacheEffectiveness() {
        // Setup test data
        for (int i = 0; i < 100; i++) {
            system.tagFile("/file" + i, List.of("tag" + (i % 20), "test"));
        }

        // Create queries that should benefit from caching
        AdvancedTaggingSystem.ComplexQuery popularQuery = new AdvancedTaggingSystem.ComplexQuery(
            Set.of("test"), Set.of(), Set.of()
        );

        // First query - cache miss
        long start1 = System.nanoTime();
        List<String> result1 = system.searchFiles(popularQuery);
        long duration1 = System.nanoTime() - start1;

        // Second query - should be cache hit
        long start2 = System.nanoTime();
        List<String> result2 = system.searchFiles(popularQuery);
        long duration2 = System.nanoTime() - start2;

        // Results should be identical
        assertEquals(result1, result2);

        // Second query should be significantly faster (cache hit)
        assertTrue(duration2 < duration1 / 2,
                  "Cached query should be at least 2x faster");

        System.out.println("First query: " + duration1 / 1_000_000 + "ms");
        System.out.println("Cached query: " + duration2 / 1_000_000 + "ms");
    }

    @Test
    @DisplayName("Should handle range queries efficiently with skip lists")
    void shouldHandleRangeQueriesWithSkipLists() {
        // Add data with lexicographically sortable tags
        system.tagFile("/file1", List.of("apple", "fruit", "red"));
        system.tagFile("/file2", List.of("banana", "fruit", "yellow"));
        system.tagFile("/file3", List.of("cherry", "fruit", "red"));
        system.tagFile("/file4", List.of("date", "fruit", "brown"));
        system.tagFile("/file5", List.of("elderberry", "fruit", "purple"));

        // Range query: tags from "b" to "d"
        AdvancedTaggingSystem.ComplexQuery rangeQuery = new AdvancedTaggingSystem.ComplexQuery(
            Set.of(), Set.of(), Set.of(), "b", "d"
        );

        List<String> results = system.searchFiles(rangeQuery);

        // Should include files with tags in range [b, d]
        assertTrue(results.contains("/file2")); // banana
        assertTrue(results.contains("/file3")); // cherry
        assertTrue(results.contains("/file4")); // date

        // Should not include files outside range
        assertFalse(results.contains("/file1")); // apple (before range)
        assertFalse(results.contains("/file5")); // elderberry (after range)
    }

    @Test
    @DisplayName("Should provide real-time analytics")
    void shouldProvideRealTimeAnalytics() throws InterruptedException {
        // Generate activity over time
        for (int i = 0; i < 50; i++) {
            system.tagFile("/batch1/file" + i, List.of("batch1", "test", "tag" + (i % 10)));
            Thread.sleep(10); // Small delay to spread over time
        }

        // Get analytics report
        AdvancedTaggingSystem.TagAnalytics.Report report =
            system.getAnalyticsReport(Duration.ofSeconds(10));

        assertNotNull(report);
        assertTrue(report.totalOperations() > 0);
        assertTrue(report.uniqueTags() >= 10); // At least tag0-tag9 plus batch1, test

        System.out.println("Analytics Report:");
        System.out.println("Total operations: " + report.totalOperations());
        System.out.println("Cache hit rate: " + report.cacheHitRate() + "%");
        System.out.println("Unique tags: " + report.uniqueTags());
    }

    @Test
    @DisplayName("Should handle memory-efficient operations at scale")
    void shouldHandleMemoryEfficientOperationsAtScale() {
        int numFiles = 10000;
        int numUniqueTags = 1000;

        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Add large dataset
        for (int i = 0; i < numFiles; i++) {
            List<String> tags = new ArrayList<>();
            // Each file gets 3-7 random tags
            int numTags = 3 + (i % 5);
            for (int j = 0; j < numTags; j++) {
                tags.add("tag" + (i * j % numUniqueTags));
            }
            system.tagFile("/largescale/file" + i, tags);

            // Occasional memory pressure check
            if (i % 1000 == 0) {
                System.gc(); // Encourage garbage collection
            }
        }

        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = endMemory - startMemory;

        System.out.println("Memory used for " + numFiles + " files: " + memoryUsed / 1024 / 1024 + " MB");

        // Verify system still performs well
        long start = System.nanoTime();
        List<AdvancedTaggingSystem.TagCount> topTags = system.getTopKTags(20);
        long duration = System.nanoTime() - start;

        assertFalse(topTags.isEmpty());
        assertTrue(duration < 10_000_000, "Top-K should complete under 10ms even with large dataset");

        // Test query performance
        AdvancedTaggingSystem.ComplexQuery query = new AdvancedTaggingSystem.ComplexQuery(
            Set.of("tag1"), Set.of(), Set.of()
        );

        start = System.nanoTime();
        List<String> queryResults = system.searchFiles(query);
        duration = System.nanoTime() - start;

        assertFalse(queryResults.isEmpty());
        assertTrue(duration < 50_000_000, "Complex query should complete under 50ms");

        System.out.println("Query performance with large dataset: " + duration / 1_000_000 + "ms");
    }

    @Test
    @DisplayName("Should demonstrate advanced algorithmic optimizations")
    void shouldDemonstrateAdvancedAlgorithmicOptimizations() {
        // Test skip list performance vs regular list
        List<String> regularTags = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            regularTags.add("tag" + String.format("%04d", i));
            system.tagFile("/perf/file" + i, List.of("tag" + String.format("%04d", i), "performance"));
        }

        // Measure search performance in ordered data
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            String searchTag = "tag" + String.format("%04d", i * 10);
            int frequency = system.getTagFrequency(searchTag);
            assertTrue(frequency >= 0);
        }
        long duration = System.nanoTime() - start;

        System.out.println("100 searches in ordered data: " + duration / 1_000_000 + "ms");
        assertTrue(duration < 100_000_000, "Searches should complete under 100ms");

        // Test that concurrent operations don't degrade performance significantly
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Long>> futures = new ArrayList<>();

        for (int t = 0; t < 10; t++) {
            futures.add(executor.submit(() -> {
                long threadStart = System.nanoTime();
                for (int i = 0; i < 50; i++) {
                    system.getTopKTags(10);
                }
                return System.nanoTime() - threadStart;
            }));
        }

        // All threads should complete reasonably quickly
        futures.forEach(future -> {
            try {
                long threadDuration = future.get();
                assertTrue(threadDuration < 500_000_000,
                          "Concurrent top-K operations should complete under 500ms");
            } catch (Exception e) {
                fail("Thread should not throw exception: " + e.getMessage());
            }
        });

        executor.shutdown();
    }

    @Test
    @DisplayName("Should validate space complexity optimizations")
    void shouldValidateSpaceComplexityOptimizations() {
        // Measure baseline memory
        System.gc();
        long baselineMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Add data with many overlapping tags (tests string interning and efficient storage)
        Set<String> commonTags = Set.of("java", "programming", "tutorial", "advanced", "beginner");

        for (int i = 0; i < 5000; i++) {
            List<String> fileTags = new ArrayList<>(commonTags);
            fileTags.add("file" + i);
            system.tagFile("/memory/file" + i, fileTags);
        }

        System.gc();
        long afterDataMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryPerFile = (afterDataMemory - baselineMemory) / 5000;

        System.out.println("Average memory per file: " + memoryPerFile + " bytes");

        // Should be efficient due to string sharing and optimized data structures
        assertTrue(memoryPerFile < 1000, "Memory per file should be under 1KB due to optimizations");

        // Verify functionality is maintained
        assertEquals(5000, system.getTagFrequency("java"));

        List<AdvancedTaggingSystem.TagCount> topTags = system.getTopKTags(10);
        assertTrue(topTags.stream().anyMatch(tc -> tc.tag().equals("java") && tc.count() == 5000));
    }
}