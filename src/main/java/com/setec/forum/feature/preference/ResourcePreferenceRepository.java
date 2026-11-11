package com.setec.forum.feature.preference;

import com.setec.forum.model.ResourcePreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ResourcePreferenceRepository extends JpaRepository<ResourcePreference, Long> {

    Optional<ResourcePreference> findByUserIdAndResourceId(Long userId, Long resourceId);

    /** Most recent choices of a user - input of the interest profile. */
    List<ResourcePreference> findTop200ByUserIdOrderByCreatedAtDesc(Long userId);

    List<ResourcePreference> findByUserIdAndResourceIdIn(Long userId, Collection<Long> resourceIds);

    @Modifying
    @Query("delete from ResourcePreference p where p.user.id = :userId and p.resource.id = :resourceId")
    void deleteByUserIdAndResourceId(@Param("userId") Long userId, @Param("resourceId") Long resourceId);

    /** Must run before a resource is deleted, otherwise the FK from resource_preferences blocks the delete. */
    @Modifying
    @Query("delete from ResourcePreference p where p.resource.id = :resourceId")
    void deleteByResourceId(@Param("resourceId") Long resourceId);
}
