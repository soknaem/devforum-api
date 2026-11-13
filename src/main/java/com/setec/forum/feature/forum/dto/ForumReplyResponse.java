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
public class ForumReplyResponse {
    private Long id;
    private Long topicId;
    private List<ForumContentBlock> blocks;
    private Long authorId;
    private String authorUsername;
    private String authorAvatarUrl;
    private boolean edited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
