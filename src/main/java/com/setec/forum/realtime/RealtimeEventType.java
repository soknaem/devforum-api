package com.setec.forum.realtime;

/**
 * Kinds of live events broadcast on {@link RealtimePublisher#RESOURCES_TOPIC}.
 */
public enum RealtimeEventType {
    /** A new PUBLISHED resource (or share) appeared in the feed. */
    RESOURCE_CREATED,
    /** A resource's content changed (title, description, tags...). Carries a full snapshot. */
    RESOURCE_UPDATED,
    /** A resource was moved to DRAFT/ARCHIVED, so it should disappear from public feeds. */
    RESOURCE_UNPUBLISHED,
    /** A resource was deleted. */
    RESOURCE_DELETED,
    COMMENT_CREATED,
    COMMENT_UPDATED,
    COMMENT_DELETED,
    /** A reaction was added / swapped / removed on a resource or a comment. */
    REACTION_CHANGED
}
