package com.setec.forum.feature.follow;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.follow.dto.response.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.setec.forum.feature.follow.service.FollowService;

@RestController
@RequestMapping("/api/v1/users/{username}")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/follow")
    public ApiResponse<Void> follow(@PathVariable String username, Authentication authentication) {
        followService.follow(username, authentication.getName());
        return ApiResponse.success("Now following " + username, null);
    }

    @DeleteMapping("/follow")
    public ApiResponse<Void> unfollow(@PathVariable String username, Authentication authentication) {
        followService.unfollow(username, authentication.getName());
        return ApiResponse.success("Unfollowed " + username, null);
    }

    @GetMapping("/followers")
    public ApiResponse<PageResponse<UserSummaryResponse>> followers(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(followService.getFollowers(username, page, size));
    }

    @GetMapping("/following")
    public ApiResponse<PageResponse<UserSummaryResponse>> following(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(followService.getFollowing(username, page, size));
    }
}
