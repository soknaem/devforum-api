package com.setec.forum.feature.forum.dto;

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
public class ForumTopicResponse {
    private Long id;
    private String title;
    private String categoryKey;
    private String categoryLabel;
    private String summary;
    private List<ForumContentBlock> blocks;
    private Long authorId;
    private String authorUsername;
    private String authorAvatarUrl;
    private long replyCount;
    private long viewCount;
    /** First image of the topic (also set in lists, where {@code blocks} is empty), or null. */
    private String coverImageUrl;
    /** The original discussion when this topic is a share, otherwise null. */
    private ForumTopicResponse sharedFrom;
    /** Computed for the current viewer (owner or admin). */
    private boolean canEdit;
    private boolean canDelete;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}