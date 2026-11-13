package com.setec.forum.feature.comment.service.impl;

import com.setec.forum.base.PageResponse;
import com.setec.forum.exception.ResourceNotFoundException;
import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.block.service.BlockService;
import com.setec.forum.feature.comment.*;
import com.setec.forum.feature.comment.dto.request.CommentRequest;
import com.setec.forum.feature.comment.dto.response.CommentResponse;
import com.setec.forum.feature.comment.service.CommentService;
import com.setec.forum.feature.notification.service.NotificationService;
import com.setec.forum.feature.reaction.ReactionRepository;
import com.setec.forum.feature.resource.ResourceRepository;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.model.Comment;
import com.setec.forum.model.NotificationType;
import com.setec.forum.model.ReactionTargetType;
import com.setec.forum.model.Resource;
import com.setec.forum.model.Role;
import com.setec.forum.model.User;
import com.setec.forum.realtime.RealtimeEvent;
import com.setec.forum.realtime.RealtimeEventType;
import com.setec.forum.realtime.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ReactionRepository reactionRepository;
    private final NotificationService notificationService;
    private final RealtimePublisher realtimePublisher;
    private final BlockService blockService;

    @Transactional
    @Override
    public CommentResponse create(Long resourceId, CommentRequest request, String currentUsername) {
        User author = requireUser(currentUsername);
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Resource", resourceId));
        assertResourceVisible(resource, author);
        if (resource.getUser() != null && blockService.isBlockedBetween(author.getId(), resource.getUser().getId())) {
            throw new IllegalArgumentException("You cannot comment on this post");
        }

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Comment", request.getParentId()));
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .resource(resource)
                .user(author)
                .parent(parent)
                .build();

        Comment saved = commentRepository.save(comment);

        if (parent != null) {
            notificationService.notify(parent.getUser(), author, NotificationType.COMMENT_REPLY,
                    "COMMENT", saved.getId(), author.getUsername() + " replied to your comment");
        } else {
            notificationService.notify(resource.getUser(), author, NotificationType.COMMENT,
                    "RESOURCE", resource.getId(), author.getUsername() + " commented on your resource \"" + resource.getTitle() + "\"");
        }

        publishCommentEvent(RealtimeEventType.COMMENT_CREATED, resource.getId(), saved.getId(),
                parent != null ? parent.getId() : null, author.getUsername());

        return toResponse(saved, author);
    }

    @Transactional
    @Override
    public PageResponse<CommentResponse> getTopLevelForResource(Long resourceId, int page, int size, String viewerUsername) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by("createdAt").descending());
        User viewer = findViewer(viewerUsername);
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Resource", resourceId));
        assertResourceVisible(resource, viewer);
        Page<Comment> comments = commentRepository.findByResourceIdAndParentIsNullAndUserIdNotIn(
                resourceId, hiddenAuthors(viewer), pageable);
        return PageResponse.from(comments.map(c -> toResponse(c, viewer)));
    }

    @Transactional
    @Override
    public PageResponse<CommentResponse> getReplies(Long parentId, int page, int size, String viewerUsername) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by("createdAt").ascending());
        User viewer = findViewer(viewerUsername);
        Page<Comment> replies = commentRepository.findByParentIdAndUserIdNotIn(parentId, hiddenAuthors(viewer), pageable);
        return PageResponse.from(replies.map(r -> toResponse(r, viewer)));
    }

    @Transactional
    @Override
    public CommentResponse update(Long commentId, CommentRequest request, String currentUsername) {
        User currentUser = requireUser(currentUsername);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Comment", commentId));

        assertOwnerOrAdmin(comment, currentUser);
        comment.setContent(request.getContent());
        comment.setEdited(true);
        Comment saved = commentRepository.save(comment);

        publishCommentEvent(RealtimeEventType.COMMENT_UPDATED, saved.getResource().getId(), saved.getId(),
                saved.getParent() != null ? saved.getParent().getId() : null, currentUser.getUsername());

        return toResponse(saved, currentUser);
    }

    @Transactional
    @Override
    public void delete(Long commentId, String currentUsername) {
        User currentUser = requireUser(currentUsername);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Comment", commentId));

        assertOwnerOrAdmin(comment, currentUser);

        // Read everything the event needs BEFORE the entity is removed.
        Long resourceId = comment.getResource().getId();
        Long parentId = comment.getParent() != null ? comment.getParent().getId() : null;

        commentRepository.delete(comment);

        publishCommentEvent(RealtimeEventType.COMMENT_DELETED, resourceId, commentId, parentId,
                currentUser.getUsername());
    }

    /** Authors whose comments this viewer must not see (blocked either way). Never empty: NOT IN () is invalid SQL. */
    private java.util.Collection<Long> hiddenAuthors(User viewer) {
        java.util.Set<Long> hidden = viewer == null ? new java.util.HashSet<>() : blockService.hiddenUserIds(viewer.getId());
        if (hidden.isEmpty()) {
            hidden.add(-1L);
        }
        return hidden;
    }

    /** Drafts and archived posts can only be seen (and discussed) by their author and admins. */
    private void assertResourceVisible(Resource resource, User viewer) {
        if (resource.getStatus() == com.setec.forum.model.ResourceStatus.PUBLISHED) {
            return;
        }
        boolean owner = viewer != null && resource.getUser() != null
                && resource.getUser().getId().equals(viewer.getId());
        boolean admin = viewer != null && viewer.getRole() == com.setec.forum.model.Role.ROLE_ADMIN;
        if (!owner && !admin) {
            throw ResourceNotFoundException.of("Resource", resource.getId());
        }
    }

    /**
     * Tells every connected client that something changed on a resource's discussion. The new total
     * is read here (the count query flushes the pending insert/delete first) so counters on feed
     * cards can be updated without a round trip.
     */
    private void publishCommentEvent(RealtimeEventType type, Long resourceId, Long commentId,
                                     Long parentCommentId, String actorUsername) {
        realtimePublisher.publish(RealtimeEvent.builder()
                .type(type)
                .resourceId(resourceId)
                .commentId(commentId)
                .parentCommentId(parentCommentId)
                .actor(actorUsername)
                .commentCount(commentRepository.countByResourceId(resourceId))
                .build());
    }

    private void assertOwnerOrAdmin(Comment comment, User currentUser) {
        if (!canModify(comment, currentUser)) {
            throw new UnauthorizedException("You do not have permission to modify this comment");
        }
    }

    /** Single source of truth for "may this user edit/delete this comment" - used by the checks AND the response flags. */
    private boolean canModify(Comment comment, User viewer) {
        if (viewer == null) {
            return false;
        }
        boolean isOwner = comment.getUser().getId().equals(viewer.getId());
        boolean isAdmin = viewer.getRole() == Role.ROLE_ADMIN;
        return isOwner || isAdmin;
    }

    private User findViewer(String viewerUsername) {
        return viewerUsername == null ? null : userRepository.findByUsername(viewerUsername).orElse(null);
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }

    private CommentResponse toResponse(Comment comment, User viewer) {
        boolean allowed = canModify(comment, viewer);
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .resourceId(comment.getResource().getId())
                .authorId(comment.getUser().getId())
                .authorUsername(comment.getUser().getUsername())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .replyCount(commentRepository.countByParentId(comment.getId()))
                .reactionCount(reactionRepository.countByTargetTypeAndTargetId(ReactionTargetType.COMMENT, comment.getId()))
                .edited(comment.isEdited())
                .canEdit(allowed)
                .canDelete(allowed)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
