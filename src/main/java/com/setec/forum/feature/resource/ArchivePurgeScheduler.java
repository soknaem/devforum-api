package com.setec.forum.feature.resource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import com.setec.forum.feature.resource.service.ResourceService;

/**
 * Archived resources are a 30-day recycle bin: the author can restore them until then, afterwards
 * they are deleted for good (comments, reactions, bookmarks and uploaded files included).
 *
 * Runs once at startup and then every night. Configure in application.yml:
 *   app.archive.retention-days: 30
 *   app.archive.purge-cron: "0 30 3 * * *"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArchivePurgeScheduler {

    private final ResourceService resourceService;

    @Value("${app.archive.retention-days:30}")
    private int retentionDays;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        purgeExpired();
    }

    @Scheduled(cron = "${app.archive.purge-cron:0 30 3 * * *}")
    public void purgeExpired() {
        try {
            // Rows archived before the archivedAt column existed start their 30 days now.
            int backfilled = resourceService.backfillArchivedAt();
            if (backfilled > 0) {
                log.info("Started the {}-day timer for {} already archived resource(s)", retentionDays, backfilled);
            }

            List<Long> expired = resourceService.findExpiredArchivedIds(retentionDays);
            int deleted = 0;
            for (Long id : expired) {
                try {
                    resourceService.purge(id); // one transaction per resource: one failure never blocks the rest
                    deleted++;
                } catch (Exception ex) {
                    log.warn("Could not purge archived resource {}: {}", id, ex.getMessage());
                }
            }
            if (deleted > 0) {
                log.info("Deleted {} archived resource(s) older than {} days", deleted, retentionDays);
            }
        } catch (Exception ex) {
            log.warn("Archive clean-up skipped: {}", ex.getMessage());
        }
    }
}
