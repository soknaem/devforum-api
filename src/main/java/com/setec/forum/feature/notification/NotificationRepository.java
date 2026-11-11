package com.setec.forum.feature.notification;

import com.setec.forum.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Notification> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from Notification n where n.resourceId = :resourceId or (n.targetType = 'RESOURCE' and n.targetId = :resourceId) or (n.targetType = 'COMMENT' and n.targetId in (select c.id from Comment c where c.resource.id = :resourceId))")
    void deleteAllForResource(
            @org.springframework.data.repository.query.Param("resourceId") Long resourceId);

    /** One-off fill of resource_id for notifications created before the column existed. */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Notification n set n.resourceId = n.targetId where n.resourceId is null and n.targetType = 'RESOURCE'")
    int backfillResourceTargets();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Notification n set n.resourceId = (select c.resource.id from Comment c where c.id = n.targetId) where n.resourceId is null and n.targetType = 'COMMENT'")
    int backfillCommentTargets();


    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndIsReadFalse(Long recipientId);
}
