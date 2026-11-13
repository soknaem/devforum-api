package com.setec.forum.feature.reaction;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.feature.reaction.dto.request.ReactionRequest;
import com.setec.forum.feature.reaction.dto.response.ReactionSummaryResponse;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.model.ReactionTargetType;
import com.setec.forum.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.setec.forum.feature.reaction.service.ReactionService;

@RestController
@RequestMapping("/api/v1/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;
    private final UserRepository userRepository;

    @PostMapping
    public ApiResponse<ReactionSummaryResponse> react(@Valid @RequestBody ReactionRequest request, Authentication authentication) {
        return ApiResponse.success("Reaction saved", reactionService.react(request, authentication.getName()));
    }

    @DeleteMapping("/{targetType}/{targetId}")
    public ApiResponse<ReactionSummaryResponse> remove(
            @PathVariable ReactionTargetType targetType,
            @PathVariable Long targetId,
            Authentication authentication) {
        return ApiResponse.success("Reaction removed", reactionService.remove(targetType, targetId, authentication.getName()));
    }

    @GetMapping("/{targetType}/{targetId}/summary")
    public ApiResponse<ReactionSummaryResponse> summary(
            @PathVariable ReactionTargetType targetType,
            @PathVariable Long targetId,
            Authentication authentication) {
        Long currentUserId = null;
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        if (isAuthenticated) {
            currentUserId = userRepository.findByUsername(authentication.getName())
                    .map(User::getId).orElse(null);
        }
        return ApiResponse.success(reactionService.summary(targetType, targetId, currentUserId));
    }
}
