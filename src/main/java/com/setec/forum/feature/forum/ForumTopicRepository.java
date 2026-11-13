package com.setec.forum.feature.forum;

import com.setec.forum.model.ForumTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ForumTopicRepository extends JpaRepository<ForumTopic, Long>, JpaSpecificationExecutor<ForumTopic> {
    @Override
    @EntityGraph(attributePaths = {"author", "author.profile", "sharedFrom", "sharedFrom.author", "sharedFrom.author.profile"})
    Page<ForumTopic> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "author.profile", "sharedFrom", "sharedFrom.author", "sharedFrom.author.profile"})
    Page<ForumTopic> findByCategoryKey(String categoryKey, Pageable pageable);

    /** Every share (repost) of the given topic. */
    List<ForumTopic> findBySharedFromId(Long topicId);

    @Modifying
    @Query("update ForumTopic topic set topic.viewCount = topic.viewCount + 1 where topic.id = :topicId")
    int incrementViewCount(@Param("topicId") Long topicId);

    @Modifying
    @Query("update ForumTopic topic set topic.replyCount = topic.replyCount + 1 where topic.id = :topicId")
    int incrementReplyCount(@Param("topicId") Long topicId);

    /** Used when a reply is removed by an admin; never goes below zero. */
    @Modifying
    @Query("update ForumTopic topic set topic.replyCount = topic.replyCount - 1 where topic.id = :topicId and topic.replyCount > 0")
    int decrementReplyCount(@Param("topicId") Long topicId);
}