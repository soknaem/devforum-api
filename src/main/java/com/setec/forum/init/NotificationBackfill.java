package com.setec.forum.init;

import com.setec.forum.feature.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Notifications created before the `resource_id` column existed get it filled in once, so the
 * "my interests" notification filter also works for them. Never blocks start-up.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class NotificationBackfill implements ApplicationRunner {

    private final NotificationService notificationService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            notificationService.backfillResourceIds();
        } catch (Exception ex) {
            log.debug("Notification resource backfill skipped: {}", ex.getMessage());
        }
    }
}
