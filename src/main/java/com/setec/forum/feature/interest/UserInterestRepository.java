package com.setec.forum.feature.interest;

import com.setec.forum.model.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

    List<UserInterest> findByUserId(Long userId);

    @Modifying
    @Query("delete from UserInterest i where i.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
