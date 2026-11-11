package com.setec.forum.feature.bookmark;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping("/api/v1/resources/{resourceId}/bookmark")
    public ApiResponse<Void> save(@PathVariable Long resourceId, Authentication authentication) {
        bookmarkService.save(resourceId, authentication.getName());
        return ApiResponse.success("Resource bookmarked", null);
    }

    @DeleteMapping("/api/v1/resources/{resourceId}/bookmark")
    public ApiResponse<Void> remove(@PathVariable Long resourceId, Authentication authentication) {
        bookmarkService.remove(resourceId, authentication.getName());
        return ApiResponse.success("Bookmark removed", null);
    }

    @GetMapping("/api/v1/users/me/bookmarks")
    public ApiResponse<PageResponse<ResourceResponse>> myBookmarks(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(bookmarkService.getMyBookmarks(authentication.getName(), page, size));
    }
}
