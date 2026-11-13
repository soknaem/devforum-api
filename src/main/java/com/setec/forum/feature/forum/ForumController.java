package com.setec.forum.feature.forum;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.forum.dto.ForumCategoryResponse;
import com.setec.forum.feature.forum.dto.ForumReplyRequest;
import com.setec.forum.feature.forum.dto.ForumReplyResponse;
import com.setec.forum.feature.forum.dto.ForumTopicRequest;
import com.setec.forum.feature.forum.dto.ForumTopicResponse;
import com.setec.forum.feature.forum.service.ForumService;
import com.setec.forum.feature.share.dto.request.ShareRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;

    @GetMapping("/categories")
    public ApiResponse<List<ForumCategoryResponse>> getCategories() {
        return ApiResponse.success(forumService.getCategories());
    }

    @GetMapping("/topics")
    public ApiResponse<PageResponse<ForumTopicResponse>> getTopics(
            @RequestParam(required = false) String categoryKey,
            @RequestParam(defaultValue = "popular") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication authentication) {
        return ApiResponse.success(forumService.getTopics(categoryKey, sortBy, page, size, usernameOf(authentication)));
    }

    @PostMapping("/topics")
    public ResponseEntity<ApiResponse<ForumTopicResponse>> createTopic(
            @Valid @RequestBody ForumTopicRequest request,
            Authentication authentication) {
        ForumTopicResponse response = forumService.createTopic(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Discussion created", response));
    }

    @GetMapping("/topics/{topicId}")
    public ApiResponse<ForumTopicResponse> getTopic(@PathVariable Long topicId, Authentication authentication) {
        return ApiResponse.success(forumService.getTopic(topicId, usernameOf(authentication)));
    }

    /** Share (repost) a discussion with an optional caption: {@code {"comment": "..."}}. */
    @PostMapping("/topics/{topicId}/share")
    public ResponseEntity<ApiResponse<ForumTopicResponse>> shareTopic(
            @PathVariable Long topicId,
            @Valid @RequestBody(required = false) ShareRequest request,
            Authentication authentication) {
        ForumTopicResponse response = forumService.shareTopic(topicId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Discussion shared", response));
    }

    /** Owner or admin only. Also removes the topic's replies and any shares of it. */
    @DeleteMapping("/topics/{topicId}")
    public ResponseEntity<ApiResponse<Void>> deleteTopic(@PathVariable Long topicId, Authentication authentication) {
        forumService.deleteTopic(topicId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Discussion deleted", null));
    }

    @GetMapping("/topics/{topicId}/replies")
    public ApiResponse<PageResponse<ForumReplyResponse>> getReplies(
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(forumService.getReplies(topicId, page, size));
    }

    @PostMapping("/topics/{topicId}/replies")
    public ResponseEntity<ApiResponse<ForumReplyResponse>> createReply(
            @PathVariable Long topicId,
            @Valid @RequestBody ForumReplyRequest request,
            Authentication authentication) {
        ForumReplyResponse response = forumService.createReply(topicId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reply created", response));
    }

    private String usernameOf(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}