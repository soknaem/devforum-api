package com.setec.forum.feature.bookmark;

import com.setec.forum.base.PageResponse;
import com.setec.forum.exception.ResourceNotFoundException;
import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.resource.ResourceRepository;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.mapper.ResourceMapper;
import com.setec.forum.model.Bookmark;
import com.setec.forum.model.Resource;
import com.setec.forum.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ResourceMapper resourceMapper;

    @Transactional
    public void save(Long resourceId, String currentUsername) {
        User user = requireUser(currentUsername);
        if (bookmarkRepository.existsByUserIdAndResourceId(user.getId(), resourceId)) {
            return; // idempotent
        }
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Resource", resourceId));
        bookmarkRepository.save(Bookmark.builder().user(user).resource(resource).build());
    }

    @Transactional
    public void remove(Long resourceId, String currentUsername) {
        User user = requireUser(currentUsername);
        bookmarkRepository.findByUserIdAndResourceId(user.getId(), resourceId)
                .ifPresent(bookmarkRepository::delete);
    }

    @Transactional
    public PageResponse<ResourceResponse> getMyBookmarks(String currentUsername, int page, int size) {
        User user = requireUser(currentUsername);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Bookmark> bookmarks = bookmarkRepository.findByUserIdAndResourceStatusOrderByCreatedAtDesc(
                user.getId(), com.setec.forum.model.ResourceStatus.PUBLISHED, pageable);
        return PageResponse.from(bookmarks.map(b -> {
            ResourceResponse response = resourceMapper.toResponse(b.getResource());
            response.setBookmarked(true);
            return response;
        }));
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }
}
