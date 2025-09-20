package com.taggingsystem.storage;

import com.taggingsystem.TagQuery;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TagStorage {
    Set<String> addTagsToFile(String filePath, Set<String> tags);
    boolean removeTagFromFile(String filePath, String tag);
    List<String> getFileTags(String filePath);
    List<String> getFilesWithTag(String tag);
    Map<String, Integer> getTagCounts();
    int getTagCount(String tag);
    List<String> getAllTags();
    List<String> clearFileTags(String filePath);
    List<String> searchFiles(TagQuery query);
}