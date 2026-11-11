package com.setec.forum.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Single place that talks to the STOMP broker.
 *
 * Messages are sent AFTER the surrounding database transaction commits. Otherwise a client could
 * receive "new comment" / "new notification" and immediately re-fetch before the row is visible,
 * and would then show stale data (or data for a transaction that later rolled back).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimePublisher {

    /** Public stream: every client subscribes to this one topic and filters by resourceId. */
    public static final String RESOURCES_TOPIC = "/topic/resources";

    /** Per-user queue. The client subscribes to /user/queue/notifications. */
    public static final String USER_NOTIFICATIONS_QUEUE = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(RealtimeEvent event) {
        runAfterCommit(() -> messagingTemplate.convertAndSend(RESOURCES_TOPIC, event));
    }

    public void sendToUser(String username, String destination, Object payload) {
        runAfterCommit(() -> messagingTemplate.convertAndSendToUser(username, destination, payload));
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeRun(task);
                }
            });
        } else {
            safeRun(task);
        }
    }

    /** A broken socket must never fail the HTTP request that triggered the event. */
    private void safeRun(Runnable task) {
        try {
            task.run();
        } catch (Exception ex) {
            log.warn("Could not push realtime message: {}", ex.getMessage());
        }
    }
}
