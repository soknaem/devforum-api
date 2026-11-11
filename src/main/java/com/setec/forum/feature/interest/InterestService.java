package com.setec.forum.feature.interest;

import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.bookmark.BookmarkRepository;
import com.setec.forum.feature.preference.ResourcePreferenceRepository;
import com.setec.forum.feature.reaction.ReactionRepository;
import com.setec.forum.feature.resource.ResourceRepository;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.feature.user.ResourceViewRepository;
import com.setec.forum.model.Bookmark;
import com.setec.forum.model.PreferenceType;
import com.setec.forum.model.Reaction;
import com.setec.forum.model.ReactionTargetType;
import com.setec.forum.model.Resource;
import com.setec.forum.model.ResourcePreference;
import com.setec.forum.model.ResourceView;
import com.setec.forum.model.Tag;
import com.setec.forum.model.User;
import com.setec.forum.model.UserInterest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a user is interested in: the tags they picked on purpose (weight 10) plus what their
 * activity teaches us - posts marked Interested (+5), saved (+4), reacted to (+3) or opened (+1),
 * and Not interested (-5). The result is a weight per tag that the For-you feed and the
 * notification filter use.
 */
@Service
@RequiredArgsConstructor
public class InterestService {

    public static final int MAX_INTERESTS = 30;

    /** weights: tag (lower-case) -> importance; explicit: tags the user picked; learned: best tags learned from activity. */
    public record InterestProfile(Map<String, Double> weights, Set<String> explicit, List<String> learned) {
    }

    private final UserInterestRepository interestRepository;
    private final UserRepository userRepository;
    private final ResourcePreferenceRepository preferenceRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ReactionRepository reactionRepository;
    private final ResourceViewRepository viewRepository;
    private final ResourceRepository resourceRepository;

    @Transactional(readOnly = true)
    public List<String> explicitTags(Long userId) {
        return interestRepository.findByUserId(userId).stream()
                .map(UserInterest::getTagName)
                .sorted()
                .toList();
    }

    @Transactional
    public List<String> replaceExplicit(String username, List<String> tags) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));

        LinkedHashSet<String> clean = new LinkedHashSet<>();
        if (tags != null) {
            for (String raw : tags) {
                if (raw == null) {
                    continue;
                }
                String name = raw.trim().toLowerCase();
                if (name.startsWith("#")) {
                    name = name.substring(1);
                }
                if (!name.isEmpty() && name.length() <= 50) {
                    clean.add(name);
                }
            }
        }
        if (clean.size() > MAX_INTERESTS) {
            throw new IllegalArgumentException("You can pick at most " + MAX_INTERESTS + " interests");
        }

        interestRepository.deleteByUserId(user.getId());
        List<UserInterest> rows = new ArrayList<>();
        for (String name : clean) {
            rows.add(UserInterest.builder().user(user).tagName(name).build());
        }
        interestRepository.saveAll(rows);
        return new ArrayList<>(clean);
    }

    @Transactional(readOnly = true)
    public List<String> popularTags(int limit) {
        return resourceRepository.findPopularTagNames(PageRequest.of(0, Math.max(1, Math.min(limit, 50))));
    }

    @Transactional(readOnly = true)
    public InterestProfile profile(Long userId) {
        Set<String> explicit = new HashSet<>();
        for (UserInterest interest : interestRepository.findByUserId(userId)) {
            explicit.add(interest.getTagName());
        }

        Map<String, Double> learned = new HashMap<>();

        for (ResourcePreference preference : preferenceRepository.findTop200ByUserIdOrderByCreatedAtDesc(userId)) {
            addTags(learned, preference.getResource(), preference.getType() == PreferenceType.INTERESTED ? 5 : -5);
        }
        for (Bookmark bookmark : bookmarkRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100)).getContent()) {
            addTags(learned, bookmark.getResource(), 4);
        }

        List<Reaction> reactions = reactionRepository
                .findTop100ByUserIdAndTargetTypeOrderByCreatedAtDesc(userId, ReactionTargetType.RESOURCE);
        if (!reactions.isEmpty()) {
            List<Long> ids = reactions.stream().map(Reaction::getTargetId).toList();
            for (Resource resource : resourceRepository.findAllById(ids)) {
                addTags(learned, resource, 3);
            }
        }
        for (ResourceView view : viewRepository
                .findByUserIdOrderByViewedAtDesc(userId, PageRequest.of(0, 100)).getContent()) {
            addTags(learned, view.getResource(), 1);
        }

        Map<String, Double> weights = new HashMap<>();
        for (String tag : explicit) {
            weights.merge(tag, 10.0, Double::sum);
        }
        learned.forEach((tag, weight) ->
                weights.merge(tag, Math.max(-10.0, Math.min(12.0, weight)), Double::sum));

        List<String> learnedTop = learned.entrySet().stream()
                .filter(e -> e.getValue() > 0 && !explicit.contains(e.getKey()))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(8)
                .map(Map.Entry::getKey)
                .toList();

        return new InterestProfile(weights, explicit, learnedTop);
    }

    /**
     * Ids of public resources the user cares about: the ones marked Interested plus everything that
     * carries one of their stronger tags. Used by the "my interests" notification filter.
     * Capped below Oracle's 1000-item IN list limit.
     */
    @Transactional(readOnly = true)
    public Set<Long> interestingResourceIds(Long userId) {
        Set<Long> ids = new LinkedHashSet<>();
        for (ResourcePreference preference : preferenceRepository.findTop200ByUserIdOrderByCreatedAtDesc(userId)) {
            if (preference.getType() == PreferenceType.INTERESTED) {
                ids.add(preference.getResource().getId());
            }
        }

        Set<String> strongTags = new HashSet<>();
        profile(userId).weights().forEach((tag, weight) -> {
            if (weight >= 5.0) {
                strongTags.add(tag);
            }
        });
        if (!strongTags.isEmpty()) {
            ids.addAll(resourceRepository.findIdsByTagNames(strongTags, PageRequest.of(0, 700)));
        }
        return ids;
    }

    private void addTags(Map<String, Double> target, Resource resource, int weight) {
        if (resource == null || resource.getTags() == null) {
            return;
        }
        for (Tag tag : resource.getTags()) {
            target.merge(tag.getName().toLowerCase(), (double) weight, Double::sum);
        }
    }
}
