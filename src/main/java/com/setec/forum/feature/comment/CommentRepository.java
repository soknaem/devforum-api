package com.setec.forum.feature.comment;

import com.setec.forum.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByResourceIdAndParentIsNull(Long resourceId, Pageable pageable);

    Page<Comment> findByParentId(Long parentId, Pageable pageable);

    /** Same lists, without the comments of the given users (blocked either way). Pass a sentinel such as -1 when empty. */
    Page<Comment> findByResourceIdAndParentIsNullAndUserIdNotIn(Long resourceId, java.util.Collection<Long> userIds, Pageable pageable);

    Page<Comment> findByParentIdAndUserIdNotIn(Long parentId, java.util.Collection<Long> userIds, Pageable pageable);

    long countByResourceId(Long resourceId);

    long countByUserId(Long userId);

    Page<Comment> findByContentContainingIgnoreCase(String content, Pageable pageable);

    @Query("select c.createdAt from Comment c where c.createdAt >= :since")
    java.util.List<java.time.LocalDateTime> findCreatedAtSince(@Param("since") java.time.LocalDateTime since);

    long countByParentId(Long parentId);

    @Query("select c.resource.id, count(c) from Comment c where c.resource.id in :resourceIds group by c.resource.id")
    List<Object[]> countByResourceIdInGrouped(@Param("resourceIds") Collection<Long> resourceIds);

    /**
     * Deletes all child replies first to prevent self-referencing FK violations (parent_id)
     */
    @Modifying
    @Query("delete from Comment c where c.resource.id = :resourceId and c.parent is not null")
    void deleteRepliesByResourceId(@Param("resourceId") Long resourceId);

    /**
     * Deletes top-level root comments for the resource
     */
    @Modifying
    @Query("delete from Comment c where c.resource.id = :resourceId")
    void deleteByResourceId(@Param("resourceId") Long resourceId);
}