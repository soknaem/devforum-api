package com.setec.forum.feature.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.setec.forum.model.NotificationType;
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
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String actorUsername;
    private String actorAvatarUrl;

    /** What targetId points at: RESOURCE, COMMENT or USER. */
    private String targetType;
    private Long targetId;

    /**
     * Where the UI should take the user when the notification is clicked. They are resolved on the
     * server so the client never has to guess (a COMMENT target only has a comment id; the page that
     * shows it is /resources/{resourceId}).
     */
    private Long resourceId;
    private Long commentId;

    /** When commentId is a reply: the top-level comment whose thread has to be opened. */
    private Long parentCommentId;

    private String message;

    /**
     * BUG FIX: a Lombok boolean called "isRead" is serialized by Jackson as "read", so the UI's
     * `n.isRead` was always undefined - which is why clicking a notification never changed its style.
     * The field is named "read" and the JSON name is pinned to "isRead".
     */
    @JsonProperty("isRead")
    private boolean read;

    private LocalDateTime createdAt;
}
