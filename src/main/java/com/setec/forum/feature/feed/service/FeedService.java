package com.setec.forum.feature.feed.service;

import com.setec.forum.base.CursorPageResponse;
import com.setec.forum.feature.feed.*;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import java.util.Collection;
import java.util.List;

public interface FeedService {
    CursorPageResponse<ResourceResponse> getFeed(Long cursor, int limit, String currentUsername);
    CursorPageResponse<ResourceResponse> getFollowingFeed(Long cursor, int limit, String currentUsername);
    CursorPageResponse<ResourceResponse> getForYou(Collection<Long> exclude, int limit, String currentUsername);
    void markSeen(List<Long> resourceIds, String currentUsername);
}
