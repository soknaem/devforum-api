package com.setec.forum.feature.notification.service;

import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.notification.*;
import com.setec.forum.feature.notification.dto.response.NotificationResponse;
import com.setec.forum.model.NotificationType;
import com.setec.forum.model.Resource;
import com.setec.forum.model.User;
import java.util.List;

public interface NotificationService {
    void deleteForResource(Long resourceId);
    void notify(User recipient, User actor, NotificationType type, String targetType, Long targetId, String message);
    void notifyFollowers(User author, Resource resource, String action, Long excludeUserId);
    PageResponse<NotificationResponse> getForUser(Long userId, int page, int size,
                                                         List<String> types, String scope, boolean unreadOnly);
    long unreadCount(Long userId);
    void backfillResourceIds();
    void markAsRead(Long notificationId, Long currentUserId);
    void markAllAsRead(Long currentUserId);
}
