package com.setec.forum.feature.feed;

import com.setec.forum.base.CursorPageResponse;
import com.setec.forum.feature.bookmark.BookmarkRepository;
import com.setec.forum.feature.comment.CommentRepository;
import com.setec.forum.feature.preference.ResourcePreferenceRepository;
import com.setec.forum.feature.reaction.ReactionRepository;
import com.setec.forum.feature.resource.ResourceRepository;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.mapper.ResourceMapper;
import com.setec.forum.model.PreferenceType;
import com.setec.forum.model.Reaction;
import com.setec.forum.model.ReactionTargetType;
import com.setec.forum.model.ReactionType;
import com.setec.forum.model.Resource;
import com.setec.forum.model.ResourcePreference;
import com.setec.forum.model.ResourceStatus;
import com.setec.forum.model.Role;
import com.setec.forum.model.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The home feeds.
 *
 *  - Latest     newest first, keyset ("infinite scroll") pagination: pass the `id` of the last item as `cursor`
 *  - Following  the same, restricted to the people the viewer follows
 *  - For you    ranked per viewer: unseen posts first, then by interest / follows / recency / engagement
 *
 * Everything the viewer marked "Not interested" and everybody they blocked (or who blocked them)
 * is left out of all three.
 */
@Service
@RequiredArgsConstructor
public class FeedService {

    /** How many recent posts "For you" ranks. Older posts are still reachable in Latest / search. */
    private static final int RANKING_POOL = 400;

    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper;
    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ResourcePreferenceRepository preferenceRepository;
    private final RecommendationService recommendationService;
    private final FeedSeenRepository feedSeenRepository;

    // ------------------------------------------------------------------ latest

    @Transactional
    public CursorPageResponse<ResourceResponse> getFeed(Long cursor, int limit, String currentUsername) {
        User viewer = findViewer(currentUsername);
        long effectiveCursor = cursor != null ? cursor : Long.MAX_VALUE;
        Pageable pageable = PageRequest.of(0, Math.max(limit, 1) + 1); // one extra: is there more?

        List<Resource> resources = viewer == null
                ? resourceRepository.findByStatusAndIdLessThanOrderByIdDesc(
                        ResourceStatus.PUBLISHED, effectiveCursor, pageable)
                : resourceRepository.findFeedExcludingHidden(
                        ResourceStatus.PUBLISHED, effectiveCursor, viewer.getId(), pageable);

        return cursorPage(resources, limit, viewer);
    }

    // --------------------------------------------------------------- following

    @Transactional
    public CursorPageResponse<ResourceResponse> getFollowingFeed(Long cursor, int limit, String currentUsername) {
        User viewer = findViewer(currentUsername);
        if (viewer == null) {
            return CursorPageResponse.<ResourceResponse>builder().content(List.of()).hasMore(false).build();
        }
        long effectiveCursor = cursor != null ? cursor : Long.MAX_VALUE;
        Pageable pageable = PageRequest.of(0, Math.max(limit, 1) + 1);

        List<Resource> resources = resourceRepository.findFollowingFeed(
                ResourceStatus.PUBLISHED, effectiveCursor, viewer.getId(), pageable);
        return cursorPage(resources, limit, viewer);
    }

    // ----------------------------------------------------------------- for you

    /**
     * @param exclude ids the client already shows. The ranking is recomputed on every request (posts
     *                get seen meanwhile), so the client says what it has instead of using an offset -
     *                that way nothing is repeated and nothing is skipped.
     */
    @Transactional
    public CursorPageResponse<ResourceResponse> getForYou(Collection<Long> exclude, int limit, String currentUsername) {
        User viewer = findViewer(currentUsername);
        if (viewer == null) {
            return getFeed(null, limit, null); // nothing to personalise for anonymous visitors
        }

        List<Resource> pool = resourceRepository.findFeedExcludingHidden(
                ResourceStatus.PUBLISHED, Long.MAX_VALUE, viewer.getId(), PageRequest.of(0, RANKING_POOL));

        Set<Long> skip = exclude == null ? Set.of() : new HashSet<>(exclude);
        List<RecommendationService.Ranked> ranked = recommendationService.rank(viewer, pool).stream()
                .filter(r -> !skip.contains(r.resource().getId()))
                .toList();

        int size = Math.max(limit, 1);
        boolean hasMore = ranked.size() > size;
        List<RecommendationService.Ranked> page = hasMore ? ranked.subList(0, size) : ranked;

        List<Resource> resources = page.stream().map(RecommendationService.Ranked::resource).toList();
        List<ResourceResponse> responses = buildResponses(resources, viewer);
        for (int i = 0; i < responses.size(); i++) {
            responses.get(i).setUnseen(page.get(i).unseen());
            responses.get(i).setReason(page.get(i).reason());
        }

        Long last = responses.isEmpty() ? null : responses.get(responses.size() - 1).getId();
        return CursorPageResponse.<ResourceResponse>builder()
                .content(responses)
                .nextCursor(hasMore ? last : null)
                .hasMore(hasMore)
                .build();
    }

    // -------------------------------------------------------------------- seen

    /**
     * The client reports posts that were on screen for a moment. They stop being "new" in For you.
     * Idempotent; unknown ids are ignored.
     */
    public void markSeen(List<Long> resourceIds, String currentUsername) {
        User viewer = findViewer(currentUsername);
        if (viewer == null || resourceIds == null || resourceIds.isEmpty()) {
            return;
        }
        List<Long> ids = resourceIds.stream().filter(java.util.Objects::nonNull).distinct().limit(50).toList();

        Set<Long> already = new HashSet<>(feedSeenRepository.findSeenResourceIds(viewer.getId(), ids));
        for (Resource resource : resourceRepository.findAllById(ids)) {
            if (already.contains(resource.getId())) {
                continue;
            }
            try {
                feedSeenRepository.save(com.setec.forum.model.FeedSeen.builder()
                        .user(viewer).resource(resource).build());
            } catch (DataIntegrityViolationException ignored) {
                // another request recorded it a moment ago
            }
        }
    }

    // ----------------------------------------------------------------- helpers

    private User findViewer(String username) {
        return username == null ? null : userRepository.findByUsername(username).orElse(null);
    }

    private CursorPageResponse<ResourceResponse> cursorPage(List<Resource> fetched, int limit, User viewer) {
        boolean hasMore = fetched.size() > limit;
        List<Resource> page = hasMore ? fetched.subList(0, limit) : fetched;
        Long nextCursor = page.isEmpty() ? null : page.get(page.size() - 1).getId();

        return CursorPageResponse.<ResourceResponse>builder()
                .content(buildResponses(page, viewer))
                .nextCursor(hasMore ? nextCursor : null)
                .hasMore(hasMore)
                .build();
    }

    /** Maps resources and adds everything that is cheap to batch: counters and the viewer's own state. */
    private List<ResourceResponse> buildResponses(List<Resource> page, User viewer) {
        List<ResourceResponse> responses = page.stream().map(resourceMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
        if (page.isEmpty()) {
            return responses;
        }

        Long currentUserId = viewer != null ? viewer.getId() : null;
        List<Long> ids = page.stream().map(Resource::getId).toList();

        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : reactionRepository.countByTargetTypeAndTargetIdInGrouped(ReactionTargetType.RESOURCE, ids)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        Map<Long, Long> commentCounts = new HashMap<>();
        for (Object[] row : commentRepository.countByResourceIdInGrouped(ids)) {
            commentCounts.put((Long) row[0], (Long) row[1]);
        }

        boolean isAdmin = viewer != null && viewer.getRole() == Role.ROLE_ADMIN;
        Map<Long, ReactionType> mine = new HashMap<>();
        Map<Long, PreferenceType> preferences = new HashMap<>();
        Map<Long, Boolean> bookmarked = new HashMap<>();
        if (currentUserId != null) {
            for (Reaction reaction : reactionRepository.findByUserIdAndTargetTypeAndTargetIdIn(
                    currentUserId, ReactionTargetType.RESOURCE, ids)) {
                mine.put(reaction.getTargetId(), reaction.getType());
            }
            for (com.setec.forum.model.Bookmark bookmark : bookmarkRepository.findByUserIdAndResourceIdIn(currentUserId, ids)) {
                bookmarked.put(bookmark.getResource().getId(), true);
            }
            for (ResourcePreference preference : preferenceRepository.findByUserIdAndResourceIdIn(currentUserId, ids)) {
                preferences.put(preference.getResource().getId(), preference.getType());
            }
        }

        for (ResourceResponse response : responses) {
            response.setReactionCount(counts.getOrDefault(response.getId(), 0L));
            response.setCommentCount(commentCounts.getOrDefault(response.getId(), 0L));
            response.setMyReaction(mine.get(response.getId()));
            response.setBookmarked(bookmarked.getOrDefault(response.getId(), false));
            response.setMyPreference(preferences.get(response.getId()));

            boolean isOwner = currentUserId != null && currentUserId.equals(response.getAuthorId());
            response.setCanEdit(isOwner || isAdmin);
            response.setCanDelete(isOwner || isAdmin);
        }
        return responses;
    }
}
