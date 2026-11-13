package com.setec.forum.feature.forum;

import com.setec.forum.model.ForumReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ForumReplyRepository extends JpaRepository<ForumReply, Long> {
    @EntityGraph(attributePaths = {"author", "author.profile"})
    Page<ForumReply> findByTopicId(Long topicId, Pageable pageable);

    @Modifying
    @Query("delete from ForumReply reply where reply.topic.id = :topicId")
    int deleteByTopicId(@Param("topicId") Long topicId);
}