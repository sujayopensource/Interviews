package com.taggingsystem.events;

@FunctionalInterface
public interface TagEventListener {
    void onTagEvent(TagEvent event);
}