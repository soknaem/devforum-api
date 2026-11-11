package com.setec.forum.feature.interest;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.interest.dto.InterestsRequest;
import com.setec.forum.feature.interest.dto.InterestsResponse;
import com.setec.forum.feature.resource.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/v1/users/me/interests   - picked tags + learned tags + suggestions
 * PUT /api/v1/users/me/interests   - replace the picked tags {"tags":["java","spring-boot"]}
 */
@RestController
@RequestMapping("/api/v1/users/me/interests")
@RequiredArgsConstructor
public class InterestController {

    private final InterestService interestService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<InterestsResponse> get(Authentication authentication) {
        return ApiResponse.success(build(authentication));
    }

    @PutMapping
    public ApiResponse<InterestsResponse> replace(@RequestBody InterestsRequest request, Authentication authentication) {
        requireLogin(authentication);
        interestService.replaceExplicit(authentication.getName(), request.getTags());
        return ApiResponse.success("Interests saved", build(authentication));
    }

    private InterestsResponse build(Authentication authentication) {
        requireLogin(authentication);
        Long userId = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"))
                .getId();

        InterestService.InterestProfile profile = interestService.profile(userId);
        java.util.List<String> picked = interestService.explicitTags(userId);
        java.util.List<String> suggestions = interestService.popularTags(20).stream()
                .filter(tag -> !picked.contains(tag))
                .toList();

        return InterestsResponse.builder()
                .tags(picked)
                .learned(profile.learned())
                .suggestions(suggestions)
                .build();
    }

    private void requireLogin(Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedException("Login required");
        }
    }
}
