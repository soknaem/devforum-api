package com.setec.forum.feature.follow.service;

import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.follow.*;
import com.setec.forum.feature.follow.dto.response.UserSummaryResponse;

public interface FollowService {
    void follow(String targetUsername, String currentUsername);
    void unfollow(String targetUsername, String currentUsername);
    PageResponse<UserSummaryResponse> getFollowers(String username, int page, int size);
    PageResponse<UserSummaryResponse> getFollowing(String username, int page, int size);
    long followerCount(String username);
    long followingCount(String username);
}
