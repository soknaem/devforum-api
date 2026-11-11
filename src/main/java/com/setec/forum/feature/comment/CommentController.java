package com.setec.forum.feature.comment;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.comment.dto.request.CommentRequest;
import com.setec.forum.feature.comment.dto.response.CommentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/api/v1/resources/{resourceId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> create(
            @PathVariable Long resourceId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        CommentResponse response = commentService.create(resourceId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Comment posted", response));
    }

    @GetMapping("/api/v1/resources/{resourceId}/comments")
    public ApiResponse<PageResponse<CommentResponse>> getTopLevel(
            @PathVariable Long resourceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return ApiResponse.success(commentService.getTopLevelForResource(resourceId, page, size, viewerOf(authentication)));
    }

    @GetMapping("/api/v1/comments/{commentId}/replies")
    public ApiResponse<PageResponse<CommentResponse>> getReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return ApiResponse.success(commentService.getReplies(commentId, page, size, viewerOf(authentication)));
    }

    @PutMapping("/api/v1/comments/{commentId}")
    public ApiResponse<CommentResponse> update(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        return ApiResponse.success("Comment updated", commentService.update(commentId, request, authentication.getName()));
    }

    /** Username of the logged-in viewer, or null for anonymous requests (public GET endpoints). */
    private String viewerOf(Authentication authentication) {
        boolean authenticated = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        return authenticated ? authentication.getName() : null;
    }

    @DeleteMapping("/api/v1/comments/{commentId}")
    public ApiResponse<Void> delete(@PathVariable Long commentId, Authentication authentication) {
        commentService.delete(commentId, authentication.getName());
        return ApiResponse.success("Comment deleted", null);
    }
}
