package com.setec.forum.feature.reaction.dto.response;

import com.setec.forum.model.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionSummaryResponse {
    private long totalCount;
    private Map<ReactionType, Long> countsByType;
    /** The requesting user's own reaction to this target, or null if they haven't reacted. */
    private ReactionType myReaction;
}
