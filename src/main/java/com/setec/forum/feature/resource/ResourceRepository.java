package com.setec.forum.feature.resource;

import com.setec.forum.model.Resource;
import com.setec.forum.model.ResourceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    List<Resource> findByStatusAndIdLessThanOrderByIdDesc(ResourceStatus status, Long cursorId, Pageable pageable);

    /**
     * Same keyset query as above, minus every resource this viewer marked "Not interested".
     * A NOT EXISTS sub-select (not a NOT IN list) so it scales and never hits Oracle's 1000-item IN limit.
     */
    @Query("""
            select r from Resource r
            where r.status = :status
              and r.id < :cursorId
              and not exists (
                  select 1 from ResourcePreference p
                  where p.resource = r
                    and p.user.id = :userId
                    and p.type = com.setec.forum.model.PreferenceType.NOT_INTERESTED)
              and not exists (
                  select 1 from UserBlock b
                  where (b.blocker.id = :userId and b.blocked.id = r.user.id)
                     or (b.blocked.id = :userId and b.blocker.id = r.user.id))
            order by r.id desc
            """)
    List<Resource> findFeedExcludingHidden(@Param("status") ResourceStatus status,
                                           @Param("cursorId") Long cursorId,
                                           @Param("userId") Long userId,
                                           Pageable pageable);

    /** Same keyset feed, restricted to the people the viewer follows ("Following" tab). */
    @Query("""
            select r from Resource r
            where r.status = :status
              and r.id < :cursorId
              and r.user.id in (select f.following.id from Follow f where f.follower.id = :userId)
              and not exists (
                  select 1 from ResourcePreference p
                  where p.resource = r
                    and p.user.id = :userId
                    and p.type = com.setec.forum.model.PreferenceType.NOT_INTERESTED)
              and not exists (
                  select 1 from UserBlock b
                  where (b.blocker.id = :userId and b.blocked.id = r.user.id)
                     or (b.blocked.id = :userId and b.blocker.id = r.user.id))
            order by r.id desc
            """)
    List<Resource> findFollowingFeed(@Param("status") ResourceStatus status,
                                     @Param("cursorId") Long cursorId,
                                     @Param("userId") Long userId,
                                     Pageable pageable);

    /** Public resources that carry one of these tags (lower-case names) - "content I am interested in". */
    @Query("select distinct r.id from Resource r join r.tags t where lower(t.name) in :names and r.status = com.setec.forum.model.ResourceStatus.PUBLISHED")
    List<Long> findIdsByTagNames(@Param("names") java.util.Collection<String> names, Pageable pageable);

    /** Most used tags on public resources, as suggestions for the interests picker. */
    @Query("select lower(t.name) from Resource r join r.tags t where r.status = com.setec.forum.model.ResourceStatus.PUBLISHED group by lower(t.name) order by count(r) desc")
    List<String> findPopularTagNames(Pageable pageable);

    /** The author's own resources in one status (Drafts / Archived pages). Sort comes from the Pageable. */
    Page<Resource> findByUserIdAndStatus(Long userId, ResourceStatus status, Pageable pageable);

    /** Archived resources older than the cutoff - candidates for the automatic clean-up. */
    @Query("select r.id from Resource r where r.status = com.setec.forum.model.ResourceStatus.ARCHIVED and r.archivedAt < :cutoff")
    List<Long> findExpiredArchivedIds(@Param("cutoff") LocalDateTime cutoff);

    /** Resources archived before archivedAt existed get their 30 days starting now. */
    @Modifying
    @Query("update Resource r set r.archivedAt = :now where r.status = com.setec.forum.model.ResourceStatus.ARCHIVED and r.archivedAt is null")
    int backfillArchivedAt(@Param("now") LocalDateTime now);

    List<Resource> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ResourceStatus status, Pageable pageable);

    long countByUserIdAndStatus(Long userId, ResourceStatus status);

    long countByUserId(Long userId);

    long countByStatus(ResourceStatus status);

    @Query("select r.createdAt from Resource r where r.createdAt >= :since")
    List<java.time.LocalDateTime> findCreatedAtSince(@Param("since") java.time.LocalDateTime since);

    @Modifying
    @Query("UPDATE Resource r SET r.sharedFrom = NULL WHERE r.sharedFrom.id = :resourceId")
    void nullifySharedFrom(@Param("resourceId") Long resourceId);
}