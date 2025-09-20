package com.taggingsystem;

import java.util.List;
import java.util.Set;

public class TagQuery {
    private final Set<String> requiredTags;
    private final Set<String> optionalTags;
    private final Set<String> excludedTags;

    public TagQuery(Set<String> requiredTags, Set<String> optionalTags, Set<String> excludedTags) {
        this.requiredTags = requiredTags != null ? Set.copyOf(requiredTags) : Set.of();
        this.optionalTags = optionalTags != null ? Set.copyOf(optionalTags) : Set.of();
        this.excludedTags = excludedTags != null ? Set.copyOf(excludedTags) : Set.of();
    }

    public static TagQuery withRequiredTags(String... tags) {
        return new TagQuery(Set.of(tags), Set.of(), Set.of());
    }

    public static TagQuery withOptionalTags(String... tags) {
        return new TagQuery(Set.of(), Set.of(tags), Set.of());
    }

    public TagQuery excluding(String... tags) {
        return new TagQuery(requiredTags, optionalTags, Set.of(tags));
    }

    public Set<String> getRequiredTags() {
        return requiredTags;
    }

    public Set<String> getOptionalTags() {
        return optionalTags;
    }

    public Set<String> getExcludedTags() {
        return excludedTags;
    }
}