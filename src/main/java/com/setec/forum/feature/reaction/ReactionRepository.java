package com.setec.forum.feature.reaction;

import com.setec.forum.model.Reaction;
import com.setec.forum.model.ReactionTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    @Modifying
    @Query("delete from Reaction r where r.targetType = :targetType and r.targetId in (select c.id from Comment c where c.resource.id = :resourceId)")
    void deleteByTargetTypeAndResourceId(
            @Param("targetType") ReactionTargetType targetType,
            @Param("resourceId") Long resourceId);

    Optional<Reaction> findByUserIdAndTargetTypeAndTargetId(Long userId, ReactionTargetType targetType, Long targetId);

    /** Latest reactions of a user - input of the interest profile. */
    List<Reaction> findTop100ByUserIdAndTargetTypeOrderByCreatedAtDesc(Long userId, ReactionTargetType targetType);

    List<Reaction> findByTargetTypeAndTargetId(ReactionTargetType targetType, Long targetId);

    long countByTargetTypeAndTargetId(ReactionTargetType targetType, Long targetId);

    @Query("select r.targetId, count(r) from Reaction r where r.targetType = :targetType and r.targetId in :targetIds group by r.targetId")
    List<Object[]> countByTargetTypeAndTargetIdInGrouped(
            @Param("targetType") ReactionTargetType targetType,
            @Param("targetIds") Collection<Long> targetIds);

    List<Reaction> findByUserIdAndTargetTypeAndTargetIdIn(
            Long userId, ReactionTargetType targetType, Collection<Long> targetIds);

    void deleteByUserIdAndTargetTypeAndTargetId(Long userId, ReactionTargetType targetType, Long targetId);

    @Modifying
    @Query("delete from Reaction r where r.targetType = :targetType and r.targetId = :targetId")
    void deleteByTargetTypeAndTargetId(
            @Param("targetType") ReactionTargetType targetType,
            @Param("targetId") Long targetId);
}