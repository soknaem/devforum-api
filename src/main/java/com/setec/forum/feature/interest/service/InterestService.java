package com.setec.forum.feature.interest.service;

import com.setec.forum.feature.interest.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface InterestService {
    public static final int MAX_INTERESTS = 30;

    public record InterestProfile(Map<String, Double> weights, Set<String> explicit, List<String> learned) {
    }

    List<String> explicitTags(Long userId);
    List<String> replaceExplicit(String username, List<String> tags);
    List<String> popularTags(int limit);
    InterestProfile profile(Long userId);
    Set<Long> interestingResourceIds(Long userId);
}
