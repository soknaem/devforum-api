package com.setec.forum.feature.notification;

import com.setec.forum.base.PageResponse;
import com.setec.forum.exception.ResourceNotFoundException;
import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.block.BlockService;
import com.setec.forum.feature.comment.CommentRepository;
import com.setec.forum.feature.interest.InterestService;
import com.setec.forum.feature.follow.FollowRepository;
import com.setec.forum.feature.notification.dto.response.NotificationResponse;
import com.setec.forum.model.Comment;
import com.setec.forum.model.Notification;
import com.setec.forum.model.NotificationType;
import com.setec.forum.model.Resource;
import com.setec.forum.model.User;
import com.setec.forum.realtime.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.data.domain.Page;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates notification rows and pushes them live to a connected user over WebSocket
 * (/user/queue/notifications) once the transaction has committed. Called from CommentService,
 * ReactionService, FollowService, and the resource "share" flow whenever an action affects someone else.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CommentRepository commentRepository;
    private final FollowRepository followRepository;
    private final RealtimePublisher realtimePublisher;
    private final BlockService blockService;
    private final InterestService interestService;

    @Transactional
    public void deleteForResource(Long resourceId) {
        notificationRepository.deleteAllForResource(resourceId);
    }

    @Transactional
    public void notify(User recipient, User actor, NotificationType type, String targetType, Long targetId, String message) {
        if (recipient.getId().equals(actor.getId())) {
            return; // don't notify users about their own actions
        }
        if (blockService.isBlockedBetween(recipient.getId(), actor.getId())) {
            return; // blocked either way: no notifications
        }

        Long resourceId = null;
        if ("RESOURCE".equals(targetType)) {
            resourceId = targetId;
        } else if ("COMMENT".equals(targetType) && targetId != null) {
            resourceId = commentRepository.findById(targetId)
                    .map(comment -> comment.getResource().getId())
                    .orElse(null);
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .targetType(targetType)
                .targetId(targetId)
                .resourceId(resourceId)
                .message(message)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        // Built now (inside the transaction), delivered after the commit.
        realtimePublisher.sendToUser(
                recipient.getUsername(),
                RealtimePublisher.USER_NOTIFICATIONS_QUEUE,
                toResponse(saved));
    }

    /**
     * "Somebody you follow posted": one real, clickable notification per follower (it is stored, shows
     * in the bell and is pushed live). People who do NOT follow the author get nothing.
     *
     * @param excludeUserId somebody who is already told about this post another way (e.g. the owner
     *                      of a shared post), or null
     */
    @Transactional
    public void notifyFollowers(User author, Resource resource, String action, Long excludeUserId) {
        String message = author.getUsername() + " " + action + " \"" + resource.getTitle() + "\"";
        for (User follower : followRepository.findFollowersOf(author.getId())) {
            if (excludeUserId != null && excludeUserId.equals(follower.getId())) {
                continue;
            }
            notify(follower, author, NotificationType.NEW_POST, "RESOURCE", resource.getId(), message);
        }
    }

    /**
     * The notification list with optional filters (all combine):
     *
     * @param types      only these kinds, e.g. COMMENT, REACTION, SHARE, FOLLOW, NEW_POST (null / empty = all kinds)
     * @param scope      ALL | FOLLOWING (done by people I follow) | INTERESTING (about posts I am interested in)
     * @param unreadOnly only notifications I have not opened
     *
     * Notifications from people I blocked (or who blocked me) are never listed.
     */
    @Transactional
    public PageResponse<NotificationResponse> getForUser(Long userId, int page, int size,
                                                         List<String> types, String scope, boolean unreadOnly) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications = notificationRepository.findAll(
                buildFilter(userId, parseTypes(types), scope, unreadOnly), pageable);
        return PageResponse.from(notifications.map(this::toResponse));
    }

    /** Unread badge: same rule as the list (blocked people do not count). */
    public long unreadCount(Long userId) {
        return notificationRepository.count(buildFilter(userId, null, "ALL", true));
    }

    private Specification<Notification> buildFilter(Long userId, List<NotificationType> types, String scope, boolean unreadOnly) {
        java.util.Set<Long> hidden = blockService.hiddenUserIds(userId);

        List<Long> actorIds = null;
        List<Long> resourceIds = null;
        if ("FOLLOWING".equalsIgnoreCase(scope)) {
            actorIds = followRepository.findFollowingIds(userId).stream().limit(900).toList();
        } else if ("INTERESTING".equalsIgnoreCase(scope)) {
            resourceIds = interestService.interestingResourceIds(userId).stream().limit(900).toList();
        }

        final List<Long> actors = actorIds;
        final List<Long> resources = resourceIds;
        return (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("recipient").get("id"), userId));
            if (types != null && !types.isEmpty()) {
                predicates.add(root.get("type").in(types));
            }
            if (unreadOnly) {
                predicates.add(cb.isFalse(root.get("isRead")));
            }
            if (actors != null) {
                predicates.add(actors.isEmpty() ? cb.disjunction() : root.get("actor").get("id").in(actors));
            }
            if (resources != null) {
                predicates.add(resources.isEmpty() ? cb.disjunction() : root.get("resourceId").in(resources));
            }
            if (!hidden.isEmpty()) {
                predicates.add(cb.not(root.get("actor").get("id").in(hidden)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<NotificationType> parseTypes(List<String> raw) {
        List<NotificationType> result = new java.util.ArrayList<>();
        if (raw != null) {
            for (String name : raw) {
                try {
                    result.add(NotificationType.valueOf(name.trim().toUpperCase()));
                } catch (IllegalArgumentException | NullPointerException ignored) {
                    // unknown kind: ignore it
                }
            }
        }
        return result;
    }

    /** Fills resource_id for notifications created before the column existed (safe to run repeatedly). */
    @Transactional
    public void backfillResourceIds() {
        notificationRepository.backfillResourceTargets();
        notificationRepository.backfillCommentTargets();
    }

    @Transactional
    public void markAsRead(Long notificationId, Long currentUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", notificationId));
        if (!notification.getRecipient().getId().equals(currentUserId)) {
            throw new UnauthorizedException("You cannot modify another user's notification");
        }
        if (notification.isRead()) {
            return; // idempotent
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long currentUserId) {
        Pageable pageable = PageRequest.of(0, 500);
        Page<Notification> page;
        do {
            page = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUserId, pageable);
            page.getContent().stream()
                    .filter(n -> !n.isRead())
                    .forEach(n -> n.setRead(true));
            notificationRepository.saveAll(page.getContent());
            pageable = pageable.next();
        } while (page.hasNext());
    }

    private NotificationResponse toResponse(Notification n) {
        Long resourceId = null;
        Long commentId = null;
        Long parentCommentId = null;

        if ("RESOURCE".equals(n.getTargetType())) {
            resourceId = n.getTargetId();
        } else if ("COMMENT".equals(n.getTargetType()) && n.getTargetId() != null) {
            // The comment may have been deleted since - then there is nowhere to route to.
            Comment comment = commentRepository.findById(n.getTargetId()).orElse(null);
            if (comment != null) {
                commentId = comment.getId();
                resourceId = comment.getResource().getId();
                parentCommentId = comment.getParent() != null ? comment.getParent().getId() : null;
            }
        }

        User actor = n.getActor();
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .actorUsername(actor.getUsername())
                .actorAvatarUrl(actor.getProfile() != null ? actor.getProfile().getAvatarUrl() : null)
                .targetType(n.getTargetType())
                .targetId(n.getTargetId())
                .resourceId(resourceId)
                .commentId(commentId)
                .parentCommentId(parentCommentId)
                .message(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
