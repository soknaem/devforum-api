package com.setec.forum.feature.resource;

import com.setec.forum.model.ResourceBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceBlockRepository extends JpaRepository<ResourceBlock, Long> {

    /** A deleted file must not stay referenced by a block (FK). */
    @Modifying
    @Query("delete from ResourceBlock b where b.media.id = :mediaId")
    void deleteByMediaId(@Param("mediaId") Long mediaId);
}
