package com.setec.forum.feature.notification;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.notification.dto.response.NotificationResponse;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getMyNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            /** Comma separated kinds: COMMENT,COMMENT_REPLY,REACTION,SHARE,FOLLOW,NEW_POST,MENTION. Empty = all. */
            @RequestParam(required = false) java.util.List<String> types,
            /** ALL (default) | FOLLOWING (people I follow) | INTERESTING (content I am interested in) */
            @RequestParam(defaultValue = "ALL") String scope,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        User user = currentUser(authentication);
        return ApiResponse.success(notificationService.getForUser(user.getId(), page, size, types, scope, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(Authentication authentication) {
        User user = currentUser(authentication);
        return ApiResponse.success(notificationService.unreadCount(user.getId()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        notificationService.markAsRead(id, user.getId());
        return ApiResponse.success("Notification marked as read", null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(Authentication authentication) {
        User user = currentUser(authentication);
        notificationService.markAllAsRead(user.getId());
        return ApiResponse.success("All notifications marked as read", null);
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
