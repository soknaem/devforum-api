package com.setec.forum.feature.block;

import com.setec.forum.model.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    List<UserBlock> findByBlockerIdOrderByCreatedAtDesc(Long blockerId);

    @Query("select b.blocked.id from UserBlock b where b.blocker.id = :userId")
    List<Long> findBlockedIds(@Param("userId") Long userId);

    @Query("select b.blocker.id from UserBlock b where b.blocked.id = :userId")
    List<Long> findBlockerIds(@Param("userId") Long userId);
}
