package com.setec.forum.feature.comment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String content;
    private Long resourceId;
    private Long authorId;
    private String authorUsername;
    private Long parentId;
    private long replyCount;
    private long reactionCount;
    private boolean edited;

    /** Whether the authenticated viewer may edit/delete this comment (owner or admin). Used by the UI to hide the buttons. */
    private boolean canEdit;
    private boolean canDelete;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
