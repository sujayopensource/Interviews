package com.taggingsystem;

import com.taggingsystem.events.TagEvent;
import com.taggingsystem.events.TagEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Staff Advanced Tagging System Tests")
class TaggingSystemTest {
    private TaggingSystem taggingSystem;

    @BeforeEach
    void setUp() {
        taggingSystem = new TaggingSystem();
    }

    @Test
    @DisplayName("Should tag file and trigger event")
    void shouldTagFileAndTriggerEvent() throws InterruptedException {
        List<TagEvent> capturedEvents = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        TagEventListener listener = event -> {
            capturedEvents.add(event);
            latch.countDown();
        };

        taggingSystem.addListener(listener);
        taggingSystem.tagFile("/file.txt", List.of("java", "programming"));

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(2, capturedEvents.size());
        assertTrue(capturedEvents.stream().anyMatch(e -> e.tag().equals("java")));
        assertTrue(capturedEvents.stream().anyMatch(e -> e.tag().equals("programming")));
    }

    @Test
    @DisplayName("Should search files with complex query")
    void shouldSearchFilesWithComplexQuery() {
        taggingSystem.tagFile("/file1.txt", List.of("java", "programming", "tutorial"));
        taggingSystem.tagFile("/file2.txt", List.of("java", "advanced", "tutorial"));
        taggingSystem.tagFile("/file3.txt", List.of("python", "programming", "tutorial"));
        taggingSystem.tagFile("/file4.txt", List.of("java", "programming", "beginner"));

        // Search for files that have java AND programming, but exclude tutorial
        TagQuery query = new TagQuery(
            Set.of("java", "programming"), // required
            Set.of(), // optional
            Set.of("tutorial") // excluded
        );

        List<String> results = taggingSystem.searchFiles(query);
        assertEquals(1, results.size());
        assertEquals("/file4.txt", results.get(0));
    }

    @Test
    @DisplayName("Should handle concurrent operations safely")
    void shouldHandleConcurrentOperationsSafely() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            final int fileIndex = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    taggingSystem.tagFile("/file" + fileIndex + ".txt", List.of("concurrent", "test"));
                    endLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(endLatch.await(10, TimeUnit.SECONDS));

        assertEquals(10, taggingSystem.getFilesWithTag("concurrent").size());
    }

    @Test
    @DisplayName("Should cache top tags and invalidate on changes")
    void shouldCacheTopTagsAndInvalidateOnChanges() {
        taggingSystem.tagFile("/file1.txt", List.of("java"));
        taggingSystem.tagFile("/file2.txt", List.of("java"));

        List<TaggingSystem.TagCount> firstCall = taggingSystem.getTopNTags(5);
        List<TaggingSystem.TagCount> secondCall = taggingSystem.getTopNTags(5);

        // Should return same result from cache
        assertEquals(firstCall, secondCall);

        // Add new tag to invalidate cache
        taggingSystem.tagFile("/file3.txt", List.of("python"));

        List<TaggingSystem.TagCount> thirdCall = taggingSystem.getTopNTags(5);
        assertEquals(2, thirdCall.size());
    }

    @Test
    @DisplayName("Should validate tag format")
    void shouldValidateTagFormat() {
        assertThrows(IllegalArgumentException.class, () ->
            taggingSystem.tagFile("/file.txt", List.of("invalid@tag")));

        assertThrows(IllegalArgumentException.class, () ->
            taggingSystem.tagFile("/file.txt", List.of("a".repeat(101))));
    }

    @Test
    @DisplayName("Should remove listeners properly")
    void shouldRemoveListenersProperly() throws InterruptedException {
        List<TagEvent> capturedEvents = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        TagEventListener listener = event -> {
            capturedEvents.add(event);
            latch.countDown();
        };

        taggingSystem.addListener(listener);
        taggingSystem.tagFile("/file.txt", List.of("java"));

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(1, capturedEvents.size());

        taggingSystem.removeListener(listener);
        taggingSystem.tagFile("/file2.txt", List.of("python"));

        // Give some time for potential event (should not happen)
        Thread.sleep(100);
        assertEquals(1, capturedEvents.size()); // Should still be 1
    }

    @Test
    @DisplayName("Should handle listener exceptions gracefully")
    void shouldHandleListenerExceptionsGracefully() throws InterruptedException {
        List<TagEvent> capturedEvents = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        TagEventListener faultyListener = event -> {
            throw new RuntimeException("Listener error");
        };

        TagEventListener goodListener = event -> {
            capturedEvents.add(event);
            latch.countDown();
        };

        taggingSystem.addListener(faultyListener);
        taggingSystem.addListener(goodListener);

        taggingSystem.tagFile("/file.txt", List.of("java"));

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(1, capturedEvents.size());
    }
}