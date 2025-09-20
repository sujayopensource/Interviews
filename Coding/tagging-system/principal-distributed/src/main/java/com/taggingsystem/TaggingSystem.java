package com.taggingsystem;

import java.util.List;

public interface TaggingSystem {
    void tagFile(String filePath, List<String> tags);
    void removeTagFromFile(String filePath, String tag);
    List<String> getFileTags(String filePath);
    List<String> getFilesWithTag(String tag);
    List<TagCount> getTopNTags(int n);
    int getTagCount(String tag);
    List<String> getAllTags();
    void clearFileTags(String filePath);

    record TagCount(String tag, int count) {}
}