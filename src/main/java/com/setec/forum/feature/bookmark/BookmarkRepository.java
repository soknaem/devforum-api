package com.setec.forum.feature.bookmark;

import com.setec.forum.model.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUserIdAndResourceId(Long userId, Long resourceId);

    boolean existsByUserIdAndResourceId(Long userId, Long resourceId);

    Page<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** Bookmarks of resources that are (still) public - a draft / archived post disappears from other people's lists. */
    Page<Bookmark> findByUserIdAndResourceStatusOrderByCreatedAtDesc(Long userId, com.setec.forum.model.ResourceStatus status, Pageable pageable);

    List<Bookmark> findByUserIdAndResourceIdIn(Long userId, Collection<Long> resourceIds);

    @Modifying
    @Query("delete from Bookmark b where b.resource.id = :resourceId")
    void deleteByResourceId(@Param("resourceId") Long resourceId);
}