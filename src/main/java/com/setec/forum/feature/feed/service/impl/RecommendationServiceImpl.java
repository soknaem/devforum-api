package com.setec.forum.feature.feed.service.impl;

import com.setec.forum.feature.comment.CommentRepository;
import com.setec.forum.feature.feed.*;
import com.setec.forum.feature.feed.service.RecommendationService;
import com.setec.forum.feature.follow.FollowRepository;
import com.setec.forum.feature.interest.service.InterestService;
import com.setec.forum.feature.reaction.ReactionRepository;
import com.setec.forum.feature.user.ResourceViewRepository;
import com.setec.forum.model.ReactionTargetType;
import com.setec.forum.model.Resource;
import com.setec.forum.model.Tag;
import com.setec.forum.model.User;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orders a pool of candidate resources for one viewer ("For you").
 *
 * 1. Resources the viewer has NOT seen yet always come first ("unread first"); their own posts and
 *    anything already shown / opened count as seen.
 * 2. Inside each group the best score wins:
 *      interest   sum of the viewer's tag weights on the resource (picked tags + learned), capped
 *      following  +15 when the author is somebody they follow
 *      recency    up to +30, halving about every 33 hours
 *      engagement 3 * ln(1 + reactions + 2 * comments)
 *
 * The explanation ("Because you like #java") is returned so the UI can show it.
 */
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {


    private final InterestService interestService;
    private final FollowRepository followRepository;
    private final FeedSeenRepository feedSeenRepository;
    private final ResourceViewRepository viewRepository;
    private final ReactionRepository reactionRepository;
    private final CommentRepository commentRepository;

    @Transactional(readOnly = true)
    @Override
    public List<RecommendationService.Ranked> rank(User viewer, List<Resource> pool) {
        if (pool.isEmpty()) {
            return List.of();
        }

        List<Long> ids = pool.stream().map(Resource::getId).toList();
        InterestService.InterestProfile profile = interestService.profile(viewer.getId());
        Set<Long> following = new HashSet<>(followRepository.findFollowingIds(viewer.getId()));

        Set<Long> seen = new HashSet<>(feedSeenRepository.findSeenResourceIds(viewer.getId(), ids));
        seen.addAll(viewRepository.findViewedResourceIds(viewer.getId(), ids));

        Map<Long, Long> reactions = new HashMap<>();
        for (Object[] row : reactionRepository.countByTargetTypeAndTargetIdInGrouped(ReactionTargetType.RESOURCE, ids)) {
            reactions.put((Long) row[0], (Long) row[1]);
        }
        Map<Long, Long> comments = new HashMap<>();
        for (Object[] row : commentRepository.countByResourceIdInGrouped(ids)) {
            comments.put((Long) row[0], (Long) row[1]);
        }

        LocalDateTime now = LocalDateTime.now();
        List<RecommendationService.Ranked> ranked = new ArrayList<>();

        for (Resource resource : pool) {
            Long authorId = resource.getUser() != null ? resource.getUser().getId() : null;

            double interest = 0;
            String bestTag = null;
            double bestWeight = 0;
            for (Tag tag : resource.getTags()) {
                String name = tag.getName().toLowerCase();
                double weight = profile.weights().getOrDefault(name, 0.0);
                interest += weight;
                if (weight > bestWeight) {
                    bestWeight = weight;
                    bestTag = name;
                }
            }
            interest = Math.max(-30.0, Math.min(30.0, interest));

            boolean followsAuthor = authorId != null && following.contains(authorId);
            double ageHours = resource.getCreatedAt() == null ? 0
                    : Math.max(0, Duration.between(resource.getCreatedAt(), now).toMinutes() / 60.0);
            double recency = 30.0 * Math.exp(-ageHours / 48.0);
            double engagement = 3.0 * Math.log1p(
                    reactions.getOrDefault(resource.getId(), 0L) + 2.0 * comments.getOrDefault(resource.getId(), 0L));

            double score = interest + (followsAuthor ? 15.0 : 0.0) + recency + engagement;

            boolean own = authorId != null && authorId.equals(viewer.getId());
            boolean unseen = !own && !seen.contains(resource.getId());

            String reason = null;
            if (bestTag != null && bestWeight >= 5.0) {
                reason = "Because you like #" + bestTag;
            } else if (followsAuthor) {
                reason = "From someone you follow";
            } else if (engagement >= 6.0) {
                reason = "Popular right now";
            }

            ranked.add(new RecommendationService.Ranked(resource, score, unseen, reason));
        }

        ranked.sort(Comparator
                .comparing(RecommendationService.Ranked::unseen).reversed()
                .thenComparing(Comparator.comparingDouble(RecommendationService.Ranked::score).reversed())
                .thenComparing((RecommendationService.Ranked r) -> r.resource().getId(), Comparator.<Long>reverseOrder()));
        return ranked;
    }
}
