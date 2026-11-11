package com.setec.forum.feature.resource;

import com.setec.forum.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<User> {

    long countByRole(com.setec.forum.model.Role role);

    long countBySuspended(Boolean suspended);

    /** Sign-up times since a date - bucketed per day in Java (portable across databases). */
    @org.springframework.data.jpa.repository.Query("select u.createdAt from User u where u.createdAt >= :since")
    java.util.List<java.time.LocalDateTime> findCreatedAtSince(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);


    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
