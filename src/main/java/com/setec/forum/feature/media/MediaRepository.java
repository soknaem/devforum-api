package com.setec.forum.feature.media;

import com.setec.forum.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByResourceId(Long resourceId);
}
