package com.setec.forum.feature.user;

import com.setec.forum.model.ResourceView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResourceViewRepository extends JpaRepository<ResourceView, Long> {

    Optional<ResourceView> findByUserIdAndResourceId(Long userId, Long resourceId);

    @EntityGraph(attributePaths = {"resource", "resource.user", "resource.user.profile"})
    Page<ResourceView> findByUserIdOrderByViewedAtDesc(Long userId, Pageable pageable);

    /** Which of these resources has the user already opened? */
    @Query("select rv.resource.id from ResourceView rv where rv.user.id = :userId and rv.resource.id in :resourceIds")
    java.util.List<Long> findViewedResourceIds(@Param("userId") Long userId, @Param("resourceIds") java.util.Collection<Long> resourceIds);

    @Modifying
    @Query("DELETE FROM ResourceView rv WHERE rv.resource.id = :resourceId")
    void deleteByResourceId(@Param("resourceId") Long resourceId);
}