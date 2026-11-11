package com.setec.forum.feature.feed;

import com.setec.forum.model.FeedSeen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface FeedSeenRepository extends JpaRepository<FeedSeen, Long> {

    @Query("select s.resource.id from FeedSeen s where s.user.id = :userId and s.resource.id in :resourceIds")
    List<Long> findSeenResourceIds(@Param("userId") Long userId, @Param("resourceIds") Collection<Long> resourceIds);
}
