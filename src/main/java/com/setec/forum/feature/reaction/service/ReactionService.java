package com.setec.forum.feature.reaction.service;

import com.setec.forum.feature.reaction.*;
import com.setec.forum.feature.reaction.dto.request.ReactionRequest;
import com.setec.forum.feature.reaction.dto.response.ReactionSummaryResponse;
import com.setec.forum.model.ReactionTargetType;

public interface ReactionService {
    ReactionSummaryResponse react(ReactionRequest request, String currentUsername);
    ReactionSummaryResponse remove(ReactionTargetType targetType, Long targetId, String currentUsername);
    ReactionSummaryResponse summary(ReactionTargetType targetType, Long targetId, Long currentUserId);
}
