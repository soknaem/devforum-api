package com.setec.forum.feature.admin;

import com.setec.forum.model.Report;
import com.setec.forum.model.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** Request / response shapes of the admin API, kept together so the panel's contract is easy to read. */
public final class AdminDtos {

    private AdminDtos() {
    }

    public record DayPoint(String date, long users, long resources, long comments) {
    }

    public record Stats(
            long users, long admins, long suspendedUsers,
            long resourcesPublished, long resourcesDraft, long resourcesArchived,
            long comments, long reactions, long openReports,
            List<DayPoint> daily) {
    }

    public record UserRow(
            Long id, String username, String email, String fullName, String avatarUrl,
            String role, boolean suspended, String suspendedReason,
            LocalDateTime createdAt, long resourceCount, long commentCount) {
    }

    public record CommentRow(
            Long id, String content, String authorUsername,
            Long resourceId, String resourceTitle, LocalDateTime createdAt) {
    }

    public record ForumTopicRow(
            Long id, String title, String categoryKey, String categoryLabel, String authorUsername,
            Long sharedFromId, long replyCount, long viewCount, LocalDateTime createdAt) {
    }

    public record ForumReplyRow(
            Long id, String preview, String authorUsername,
            Long topicId, String topicTitle, LocalDateTime createdAt) {
    }

    public record ReportRow(
            Long id, String targetType, Long targetId, String targetLabel, String targetOwner,
            Long resourceId, String reason, String details, String status,
            String reporter, String reviewedBy, String reviewNote,
            LocalDateTime createdAt, LocalDateTime reviewedAt) {
    }

    public record AuditRow(
            Long id, String admin, String action, String targetType, Long targetId,
            String details, LocalDateTime createdAt) {
    }

    // ---- requests ----

    public record RoleRequest(@NotNull(message = "role is required") Role role) {
    }

    public record SuspendRequest(@Size(max = 300, message = "Reason must not exceed 300 characters") String reason) {
    }

    /**
     * @param action optional follow-up on the reported item: ARCHIVE or DELETE (post), DELETE (comment),
     *               SUSPEND (user), NONE
     */
    public record ResolveRequest(
            @NotNull(message = "status is required (RESOLVED or DISMISSED)") Report.Status status,
            @Size(max = 300, message = "Note must not exceed 300 characters") String note,
            String action) {
    }
}