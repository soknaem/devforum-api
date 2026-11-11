package com.setec.forum.realtime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.model.ReactionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Lightweight, viewer-independent payload pushed to every connected client on
 * {@code /topic/resources}. It never contains anything that depends on who is looking
 * (myReaction, bookmarked, canEdit...) - clients keep those from their own HTTP responses and only
 * merge the shared parts (counts, content) from the event.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RealtimeEvent {

    private final RealtimeEventType type;

    /** The resource this event is about (for comment/reaction-on-comment events: the comment's resource). */
    private final Long resourceId;

    private final Long commentId;

    /** Set when {@link #commentId} is a reply: the top-level comment it belongs to. */
    private final Long parentCommentId;

    /** REACTION_CHANGED only: "RESOURCE" or "COMMENT", plus the id of the reacted target. */
    private final String targetType;
    private final Long targetId;

    /** Username of the person who triggered the event. */
    private final String actor;

    /** Total comments (replies included) on the resource, after the change. */
    private final Long commentCount;

    /** REACTION_CHANGED only: total reactions on the target (see targetType), after the change. */
    private final Long reactionCount;

    private final Map<ReactionType, Long> countsByType;

    /** RESOURCE_UPDATED only: snapshot of the resource without any viewer-specific fields. */
    private final ResourceResponse resource;

    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();
}
