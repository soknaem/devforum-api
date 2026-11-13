package com.setec.forum.feature.feed.service;

import com.setec.forum.feature.feed.*;
import com.setec.forum.model.Resource;
import com.setec.forum.model.User;
import java.util.List;

public interface RecommendationService {
    public record Ranked(Resource resource, double score, boolean unseen, String reason) {
    }

    List<Ranked> rank(User viewer, List<Resource> pool);
}
