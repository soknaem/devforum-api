package com.setec.forum.feature.reaction;

import com.setec.forum.exception.ResourceNotFoundException;
import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.comment.CommentRepository;
import com.setec.forum.feature.notification.NotificationService;
import com.setec.forum.feature.reaction.dto.request.ReactionRequest;
import com.setec.forum.feature.reaction.dto.response.ReactionSummaryResponse;
import com.setec.forum.feature.resource.ResourceRepository;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.model.Comment;
import com.setec.forum.model.NotificationType;
import com.setec.forum.model.Reaction;
import com.setec.forum.model.ReactionTargetType;
import com.setec.forum.model.ReactionType;
import com.setec.forum.model.Resource;
import com.setec.forum.model.User;
import com.setec.forum.realtime.RealtimeEvent;
import com.setec.forum.realtime.RealtimeEventType;
import com.setec.forum.realtime.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final ResourceRepository resourceRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RealtimePublisher realtimePublisher;

    /** Upserts the current user's reaction on a target. Re-reacting with a different type just swaps it. */
    @Transactional
    public ReactionSummaryResponse react(ReactionRequest request, String currentUsername) {
        User user = requireUser(currentUsername);
        validateTargetExists(request.getTargetType(), request.getTargetId());

        Reaction reaction = reactionRepository
                .findByUserIdAndTargetTypeAndTargetId(user.getId(), request.getTargetType(), request.getTargetId())
                .orElse(Reaction.builder()
                        .user(user)
                        .targetType(request.getTargetType())
                        .targetId(request.getTargetId())
                        .build());

        boolean isNew = reaction.getId() == null;
        reaction.setType(request.getType());
        reactionRepository.save(reaction);

        if (isNew) {
            notifyOwner(request.getTargetType(), request.getTargetId(), user);
        }

        ReactionSummaryResponse result = summary(request.getTargetType(), request.getTargetId(), user.getId());
        publishReactionEvent(request.getTargetType(), request.getTargetId(), user, result);
        return result;
    }

    @Transactional
    public ReactionSummaryResponse remove(ReactionTargetType targetType, Long targetId, String currentUsername) {
        User user = requireUser(currentUsername);
        reactionRepository.deleteByUserIdAndTargetTypeAndTargetId(user.getId(), targetType, targetId);
        ReactionSummaryResponse result = summary(targetType, targetId, user.getId());
        publishReactionEvent(targetType, targetId, user, result);
        return result;
    }

    @Transactional
    public ReactionSummaryResponse summary(ReactionTargetType targetType, Long targetId, Long currentUserId) {
        List<Reaction> reactions = reactionRepository.findByTargetTypeAndTargetId(targetType, targetId);

        Map<ReactionType, Long> counts = new EnumMap<>(ReactionType.class);
        ReactionType mine = null;
        for (Reaction r : reactions) {
            counts.merge(r.getType(), 1L, Long::sum);
            if (currentUserId != null && r.getUser().getId().equals(currentUserId)) {
                mine = r.getType();
            }
        }

        return ReactionSummaryResponse.builder()
                .totalCount(reactions.size())
                .countsByType(counts)
                .myReaction(mine)
                .build();
    }

    /**
     * Broadcasts the new totals. For a COMMENT target the event also carries the comment's resource
     * (and its parent thread) so clients know which discussion to refresh. The reactor's own
     * reaction type is deliberately left out - that is per-viewer data.
     */
    private void publishReactionEvent(ReactionTargetType targetType, Long targetId, User actor,
                                      ReactionSummaryResponse summary) {
        Long resourceId = null;
        Long commentId = null;
        Long parentCommentId = null;

        if (targetType == ReactionTargetType.RESOURCE) {
            resourceId = targetId;
        } else {
            Comment comment = commentRepository.findById(targetId).orElse(null);
            if (comment == null) {
                return;
            }
            commentId = comment.getId();
            resourceId = comment.getResource().getId();
            parentCommentId = comment.getParent() != null ? comment.getParent().getId() : null;
        }

        realtimePublisher.publish(RealtimeEvent.builder()
                .type(RealtimeEventType.REACTION_CHANGED)
                .resourceId(resourceId)
                .commentId(commentId)
                .parentCommentId(parentCommentId)
                .targetType(targetType.name())
                .targetId(targetId)
                .actor(actor.getUsername())
                .reactionCount(summary.getTotalCount())
                .countsByType(summary.getCountsByType())
                .build());
    }

    private void validateTargetExists(ReactionTargetType targetType, Long targetId) {
        boolean exists = switch (targetType) {
            case RESOURCE -> resourceRepository.existsById(targetId);
            case COMMENT -> commentRepository.existsById(targetId);
        };
        if (!exists) {
            throw ResourceNotFoundException.of(targetType.name(), targetId);
        }
    }

    private void notifyOwner(ReactionTargetType targetType, Long targetId, User actor) {
        if (targetType == ReactionTargetType.RESOURCE) {
            Resource resource = resourceRepository.findById(targetId).orElse(null);
            if (resource != null) {
                notificationService.notify(resource.getUser(), actor, NotificationType.REACTION,
                        "RESOURCE", resource.getId(), actor.getUsername() + " reacted to your resource \"" + resource.getTitle() + "\"");
            }
        } else {
            Comment comment = commentRepository.findById(targetId).orElse(null);
            if (comment != null) {
                notificationService.notify(comment.getUser(), actor, NotificationType.REACTION,
                        "COMMENT", comment.getId(), actor.getUsername() + " reacted to your comment");
            }
        }
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }
}
