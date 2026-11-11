package com.setec.forum.feature.follow;

import com.setec.forum.model.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    /** Everybody who follows `userId` (the audience of a new post). */
    @org.springframework.data.jpa.repository.Query("select f.follower from Follow f where f.following.id = :userId")
    java.util.List<com.setec.forum.model.User> findFollowersOf(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("select f.following.id from Follow f where f.follower.id = :userId")
    java.util.List<Long> findFollowingIds(@org.springframework.data.repository.query.Param("userId") Long userId);

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Page<Follow> findByFollowingId(Long followingId, Pageable pageable);

    Page<Follow> findByFollowerId(Long followerId, Pageable pageable);

    long countByFollowingId(Long followingId);

    long countByFollowerId(Long followerId);
}
