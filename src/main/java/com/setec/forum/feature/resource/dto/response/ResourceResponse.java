package com.setec.forum.feature.resource.dto.response;

import com.setec.forum.model.PreferenceType;
import com.setec.forum.model.ReactionType;
import com.setec.forum.model.ResourceStatus;
import com.setec.forum.model.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponse {

    private Long id;
    private String title;
    private String description;
    private String url;
    private ResourceType type;
    private ResourceStatus status;

    private Long categoryId;
    private String categoryName;

    private Long authorId;
    private String authorUsername;

    private List<String> tags;

    private List<com.setec.forum.feature.media.dto.response.MediaResponse> media;

    /**
     * Ordered body of the post (text and files in the author's order). Only filled by the single
     * resource endpoint and by create/update responses - lists leave it null; use {@link #media} there.
     */
    private List<BlockResponse> blocks;

    private long viewCount;
    private long commentCount;
    private long reactionCount;
    private boolean bookmarked;

    /** The authenticated viewer's reaction to this resource, or null when not reacted/anonymous. */
    private ReactionType myReaction;

    /** The viewer's "Interested" / "Not interested" choice for this resource, or null. */
    private PreferenceType myPreference;

    /**
     * Whether the authenticated viewer may edit / delete this resource (owner or admin). Computed by
     * the API so the "..." menu can decide which actions to show; false for anonymous viewers.
     * Only filled by the feed and the single-resource endpoint - other lists leave them false and the
     * UI falls back to comparing authorId with the logged-in user (the API still enforces it).
     */
    /**
     * "For you" feed only: true while the viewer has not seen this post yet (shown as "New"), and the
     * reason it was recommended ("Because you like #java").
     */
    private boolean unseen;
    private String reason;

    private boolean canEdit;
    private boolean canDelete;

    /** Set when this resource is a share/repost of another one. */
    private Long sharedFromId;
    private String sharedFromTitle;
    private String sharedFromAuthorUsername;

    /**
     * Full content of the original post (author, title, description, media, tags, url...) when this
     * resource is a share, so the UI can render it Facebook-style. Never nested more than one level:
     * the original's own {@code sharedFrom} is always null.
     */
    private ResourceResponse sharedFrom;

    /** ARCHIVED only: when it was archived, and when it will be deleted automatically. */
    private LocalDateTime archivedAt;
    private LocalDateTime purgeAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
