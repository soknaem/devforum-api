package com.setec.forum.feature.share.service.impl;

import com.setec.forum.exception.ResourceNotFoundException;
import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.notification.service.NotificationService;
import com.setec.forum.feature.resource.ResourceRepository;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.feature.share.*;
import com.setec.forum.feature.share.dto.request.ShareRequest;
import com.setec.forum.feature.share.service.ShareService;
import com.setec.forum.mapper.ResourceMapper;
import com.setec.forum.model.NotificationType;
import com.setec.forum.model.Resource;
import com.setec.forum.model.ResourceStatus;
import com.setec.forum.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A "share" is modeled as a brand-new Resource row that points back at the original via
 * {@link Resource#getSharedFrom()} - so shares show up in feeds/history/user-resource-lists for
 * free, the same way an original post would (Facebook-style repost, not a separate table).
 */
@Service
@RequiredArgsConstructor
public class ShareServiceImpl implements ShareService {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ResourceMapper resourceMapper;
    private final NotificationService notificationService;

    @Transactional
    @Override
    public ResourceResponse share(Long resourceId, ShareRequest request, String currentUsername) {
        User sharer = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
        Resource original = resourceRepository.findById(resourceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Resource", resourceId));

        // Sharing a share (Facebook-style) re-shares the ORIGINAL post, so shares never nest.
        if (original.getSharedFrom() != null) {
            original = original.getSharedFrom();
        }

        // The share now embeds the full original content, so never expose drafts/archived posts.
        if (original.getStatus() != ResourceStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only published resources can be shared");
        }

        Resource share = Resource.builder()
                .title(original.getTitle())
                // The sharer's own caption. The original's content is NOT copied - it is read live
                // through sharedFrom, so the shared post always shows the full, up-to-date original.
                .description(request != null && request.getComment() != null && !request.getComment().isBlank()
                        ? request.getComment().trim() : null)
                .url(original.getUrl())
                .type(original.getType())
                .status(ResourceStatus.PUBLISHED)
                .user(sharer)
                .category(original.getCategory())
                .sharedFrom(original)
                .build();

        Resource saved = resourceRepository.save(share);

        notificationService.notify(original.getUser(), sharer, NotificationType.SHARE,
                "RESOURCE", original.getId(), sharer.getUsername() + " shared your resource \"" + original.getTitle() + "\"");

        // A share is a post in the sharer's followers' feeds. The original's owner already receives
        // the SHARE notification above, so they are not told twice.
        notificationService.notifyFollowers(sharer, saved, "shared a post", original.getUser().getId());

        return resourceMapper.toResponse(saved);
    }
}
