package com.setec.forum.feature.user;

import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.mapper.ResourceMapper;
import com.setec.forum.model.Resource;
import com.setec.forum.model.ResourceView;
import com.setec.forum.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Powers the "recently viewed" history feature. One row per (user, resource): repeat views just
 * bump `viewedAt` rather than growing the table unboundedly.
 */
@Service
@RequiredArgsConstructor
public class ResourceViewService {

    private final ResourceViewRepository resourceViewRepository;
    private final UserRepository userRepository;
    private final ResourceMapper resourceMapper;

    @Transactional
    public void recordView(Resource resource, String viewerUsername) {
        if (viewerUsername == null) {
            return; // anonymous view - nothing to record against
        }
        userRepository.findByUsername(viewerUsername).ifPresent(user -> {
            ResourceView view = resourceViewRepository.findByUserIdAndResourceId(user.getId(), resource.getId())
                    .orElse(ResourceView.builder().user(user).resource(resource).build());
            view.setViewedAt(LocalDateTime.now());
            resourceViewRepository.save(view);
        });
    }

    @Transactional
    public PageResponse<ResourceResponse> getHistory(String username, int page, int size) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<ResourceView> views = resourceViewRepository.findByUserIdOrderByViewedAtDesc(user.getId(), pageable);
        return PageResponse.from(views.map(v -> resourceMapper.toResponse(v.getResource())));
    }
}
