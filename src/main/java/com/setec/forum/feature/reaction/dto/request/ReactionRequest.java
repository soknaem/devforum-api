package com.setec.forum.feature.reaction.dto.request;

import com.setec.forum.model.ReactionTargetType;
import com.setec.forum.model.ReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionRequest {

    @NotNull(message = "targetType is required")
    private ReactionTargetType targetType;

    @NotNull(message = "targetId is required")
    private Long targetId;

    @NotNull(message = "type is required")
    private ReactionType type;
}
