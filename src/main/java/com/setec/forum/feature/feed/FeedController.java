package com.setec.forum.feature.feed;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.base.CursorPageResponse;
import com.setec.forum.feature.feed.dto.SeenRequest;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /**
     * Latest first. First call: GET /api/v1/feed?limit=20.
     * Next call: GET /api/v1/feed?cursor={nextCursor from previous response}&limit=20.
     */
    @GetMapping
    public ApiResponse<CursorPageResponse<ResourceResponse>> getFeed(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {
        return ApiResponse.success(feedService.getFeed(cursor, limit, usernameOf(authentication)));
    }

    /** Only people I follow, latest first. Same cursor paging as above. */
    @GetMapping("/following")
    public ApiResponse<CursorPageResponse<ResourceResponse>> getFollowing(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {
        return ApiResponse.success(feedService.getFollowingFeed(cursor, limit, usernameOf(authentication)));
    }

    /**
     * Ranked for me: unseen posts first, then by my interests. Send the ids already on screen as
     * `exclude` (comma separated) to get the next batch: GET /api/v1/feed/for-you?exclude=5,9,12&limit=20
     */
    @GetMapping("/for-you")
    public ApiResponse<CursorPageResponse<ResourceResponse>> getForYou(
            @RequestParam(required = false) List<Long> exclude,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {
        return ApiResponse.success(feedService.getForYou(exclude, limit, usernameOf(authentication)));
    }

    /** Posts that were visible on screen: they stop being "new" in For you. */
    @PostMapping("/seen")
    public ApiResponse<Void> markSeen(@RequestBody SeenRequest request, Authentication authentication) {
        feedService.markSeen(request.getResourceIds(), usernameOf(authentication));
        return ApiResponse.success("Recorded", null);
    }

    private String usernameOf(Authentication authentication) {
        boolean authenticated = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        return authenticated ? authentication.getName() : null;
    }
}
