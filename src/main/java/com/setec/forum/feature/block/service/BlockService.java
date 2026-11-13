package com.setec.forum.feature.block.service;

import com.setec.forum.feature.block.*;
import com.setec.forum.feature.follow.dto.response.UserSummaryResponse;
import java.util.List;
import java.util.Set;

public interface BlockService {
    void block(String targetUsername, String currentUsername);
    void unblock(String targetUsername, String currentUsername);
    boolean isBlockedByMe(String currentUsername, String targetUsername);
    List<UserSummaryResponse> listBlocked(String currentUsername);
    Set<Long> hiddenUserIds(Long userId);
    boolean isBlockedBetween(Long userA, Long userB);
}
