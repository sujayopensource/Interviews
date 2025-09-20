package com.taggingsystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SE2 Basic Tagging System Tests")
class TaggingSystemTest {
    private TaggingSystem taggingSystem;

    @BeforeEach
    void setUp() {
        taggingSystem = new TaggingSystem();
    }

    @Test
    @DisplayName("Should tag file successfully")
    void shouldTagFileSuccessfully() {
        String filePath = "/path/to/file.txt";
        List<String> tags = List.of("java", "programming", "tutorial");

        taggingSystem.tagFile(filePath, tags);

        List<String> retrievedTags = taggingSystem.getFileTags(filePath);
        assertEquals(3, retrievedTags.size());
        assertTrue(retrievedTags.containsAll(List.of("java", "programming", "tutorial")));
    }

    @Test
    @DisplayName("Should normalize tags to lowercase")
    void shouldNormalizeTagsToLowercase() {
        String filePath = "/path/to/file.txt";
        List<String> tags = List.of("JAVA", "Programming", "TUTORIAL");

        taggingSystem.tagFile(filePath, tags);

        List<String> retrievedTags = taggingSystem.getFileTags(filePath);
        assertTrue(retrievedTags.containsAll(List.of("java", "programming", "tutorial")));
    }

    @Test
    @DisplayName("Should remove duplicate tags")
    void shouldRemoveDuplicateTags() {
        String filePath = "/path/to/file.txt";
        List<String> tags = List.of("java", "java", "programming");

        taggingSystem.tagFile(filePath, tags);

        List<String> retrievedTags = taggingSystem.getFileTags(filePath);
        assertEquals(2, retrievedTags.size());
        assertTrue(retrievedTags.containsAll(List.of("java", "programming")));
    }

    @Test
    @DisplayName("Should remove tag from file")
    void shouldRemoveTagFromFile() {
        String filePath = "/path/to/file.txt";
        List<String> tags = List.of("java", "programming", "tutorial");

        taggingSystem.tagFile(filePath, tags);
        taggingSystem.removeTagFromFile(filePath, "programming");

        List<String> retrievedTags = taggingSystem.getFileTags(filePath);
        assertEquals(2, retrievedTags.size());
        assertFalse(retrievedTags.contains("programming"));
    }

    @Test
    @DisplayName("Should get files with specific tag")
    void shouldGetFilesWithSpecificTag() {
        taggingSystem.tagFile("/file1.txt", List.of("java", "programming"));
        taggingSystem.tagFile("/file2.txt", List.of("python", "programming"));
        taggingSystem.tagFile("/file3.txt", List.of("java", "tutorial"));

        List<String> javaFiles = taggingSystem.getFilesWithTag("java");
        assertEquals(2, javaFiles.size());
        assertTrue(javaFiles.containsAll(List.of("/file1.txt", "/file3.txt")));

        List<String> programmingFiles = taggingSystem.getFilesWithTag("programming");
        assertEquals(2, programmingFiles.size());
        assertTrue(programmingFiles.containsAll(List.of("/file1.txt", "/file2.txt")));
    }

    @Test
    @DisplayName("Should get top N tags")
    void shouldGetTopNTags() {
        taggingSystem.tagFile("/file1.txt", List.of("java", "programming"));
        taggingSystem.tagFile("/file2.txt", List.of("java", "tutorial"));
        taggingSystem.tagFile("/file3.txt", List.of("java", "advanced"));
        taggingSystem.tagFile("/file4.txt", List.of("python", "programming"));

        List<TaggingSystem.TagCount> topTags = taggingSystem.getTopNTags(3);

        assertEquals(3, topTags.size());
        assertEquals("java", topTags.get(0).tag());
        assertEquals(3, topTags.get(0).count());
        assertEquals("programming", topTags.get(1).tag());
        assertEquals(2, topTags.get(1).count());
    }

    @Test
    @DisplayName("Should clear all tags from file")
    void shouldClearAllTagsFromFile() {
        String filePath = "/path/to/file.txt";
        List<String> tags = List.of("java", "programming", "tutorial");

        taggingSystem.tagFile(filePath, tags);
        taggingSystem.clearFileTags(filePath);

        List<String> retrievedTags = taggingSystem.getFileTags(filePath);
        assertTrue(retrievedTags.isEmpty());

        // Verify tags are removed from tag-to-files mapping
        assertEquals(0, taggingSystem.getFilesWithTag("java").size());
    }

    @Test
    @DisplayName("Should throw exception for null file path")
    void shouldThrowExceptionForNullFilePath() {
        assertThrows(IllegalArgumentException.class, () ->
            taggingSystem.tagFile(null, List.of("tag")));
    }

    @Test
    @DisplayName("Should throw exception for empty tags")
    void shouldThrowExceptionForEmptyTags() {
        assertThrows(IllegalArgumentException.class, () ->
            taggingSystem.tagFile("/file.txt", List.of()));
    }

    @Test
    @DisplayName("Should throw exception for negative N in getTopNTags")
    void shouldThrowExceptionForNegativeN() {
        assertThrows(IllegalArgumentException.class, () ->
            taggingSystem.getTopNTags(-1));
    }

    @Test
    @DisplayName("Should handle empty whitespace tags")
    void shouldHandleEmptyWhitespaceTags() {
        String filePath = "/path/to/file.txt";
        List<String> tags = List.of("java", "  ", "programming", "");

        taggingSystem.tagFile(filePath, tags);

        List<String> retrievedTags = taggingSystem.getFileTags(filePath);
        assertEquals(2, retrievedTags.size());
        assertTrue(retrievedTags.containsAll(List.of("java", "programming")));
    }
}