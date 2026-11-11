package com.setec.forum.feature.resource;

import com.setec.forum.base.PageResponse;
import com.setec.forum.exception.ResourceNotFoundException;
import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.bookmark.BookmarkRepository;
import com.setec.forum.feature.comment.CommentRepository;
import com.setec.forum.feature.block.BlockService;
import com.setec.forum.feature.feed.FeedSeenRepository;
import com.setec.forum.feature.media.MediaService;
import com.setec.forum.feature.notification.NotificationService;
import com.setec.forum.feature.preference.ResourcePreferenceRepository;
import com.setec.forum.feature.reaction.ReactionRepository;
import com.setec.forum.feature.resource.dto.request.BlockRequest;
import com.setec.forum.feature.resource.dto.request.ResourceRequest;
import com.setec.forum.feature.resource.dto.response.BlockResponse;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.feature.user.ResourceViewRepository;
import com.setec.forum.feature.user.ResourceViewService;
import com.setec.forum.mapper.ResourceMapper;
import com.setec.forum.model.BlockType;
import com.setec.forum.model.Category;
import com.setec.forum.model.Media;
import com.setec.forum.model.ReactionTargetType;
import com.setec.forum.model.Resource;
import com.setec.forum.model.ResourceBlock;
import com.setec.forum.model.ResourceStatus;
import com.setec.forum.model.ResourceType;
import com.setec.forum.model.Role;
import com.setec.forum.model.Tag;
import com.setec.forum.model.User;
import com.setec.forum.realtime.RealtimeEvent;
import com.setec.forum.realtime.RealtimeEventType;
import com.setec.forum.realtime.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final ResourceMapper resourceMapper;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ResourceViewService resourceViewService;
    private final ResourceViewRepository resourceViewRepository;
    private final ResourcePreferenceRepository preferenceRepository;
    private final RealtimePublisher realtimePublisher;
    private final NotificationService notificationService;
    private final MediaService mediaService;
    private final BlockService blockService;
    private final FeedSeenRepository feedSeenRepository;

    /** Archived resources are deleted automatically after this many days. */
    @Value("${app.archive.retention-days:30}")
    private int archiveRetentionDays;

    private static final int MAX_BLOCKS = 50;
    private static final int MAX_BLOCK_TEXT = 4000;

    @Override
    @Transactional
    public ResourceResponse create(ResourceRequest request, String currentUsername) {
        User author = resolveAuthor(request.getUserId(), currentUsername);

        Resource resource = resourceMapper.toEntity(request);
        resource.setUser(author);
        applyStatus(resource, request.getStatus() != null ? request.getStatus() : ResourceStatus.PUBLISHED);
        applyCategory(resource, request.getCategoryId());
        applyTags(resource, request.getTags());

        Resource saved = resourceRepository.save(resource);

        // Followers (only) get a real notification; nothing is broadcast to everybody.
        if (saved.getStatus() == ResourceStatus.PUBLISHED) {
            notificationService.notifyFollowers(author, saved, "published a new resource", null);
        }

        return enrich(resourceMapper.toResponse(saved), saved, author.getUsername());
    }

    @Override
    @Transactional
    public ResourceResponse getById(Long id) {
        return getById(id, null);
    }

    @Override
    @Transactional
    public ResourceResponse getById(Long id, String viewerUsername) {
        Resource resource = findResourceOrThrow(id);

        // Drafts and archived posts do not exist for anybody but their author (and admins).
        if (resource.getStatus() != ResourceStatus.PUBLISHED && !canManage(resource, viewerUsername)) {
            throw ResourceNotFoundException.of("Resource", id);
        }

        resource.setViewCount(resource.getViewCount() + 1);
        resourceRepository.save(resource);
        resourceViewService.recordView(resource, viewerUsername);

        return withBlocks(enrich(resourceMapper.toResponse(resource), resource, viewerUsername), resource);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceResponse refresh(Long id) {
        Resource resource = findResourceOrThrow(id);
        return withBlocks(enrich(resourceMapper.toResponse(resource), resource, null), resource);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> getAll(String search, Long categoryId, ResourceType type,
                                                 ResourceStatus status, String tag,
                                                 int page, int size, String sortBy, String sortDir,
                                                 String viewerUsername) {
        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy != null ? sortBy : "createdAt").descending()
                : Sort.by(sortBy != null ? sortBy : "createdAt").ascending();

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort);

        // The public list only ever contains PUBLISHED resources; the `status` filter is ignored on purpose,
        // otherwise anybody could list other people's drafts. Authors use getMine() instead.
        Specification<Resource> spec = ResourceSpecification.buildFilter(search, categoryId, type, ResourceStatus.PUBLISHED, tag);
        // Nobody sees posts of people they blocked (or who blocked them) in search results either.
        if (viewerUsername != null) {
            java.util.Set<Long> hidden = userRepository.findByUsername(viewerUsername)
                    .map(viewer -> blockService.hiddenUserIds(viewer.getId()))
                    .orElse(java.util.Set.of());
            spec = spec.and(ResourceSpecification.notAuthoredBy(hidden));
        }
        Page<Resource> resourcePage = resourceRepository.findAll(spec, pageable);

        Page<ResourceResponse> responsePage = resourcePage.map(r -> enrich(resourceMapper.toResponse(r), r));
        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request, String currentUsername) {
        Resource resource = findResourceOrThrow(id);
        assertOwnerOrAdmin(resource, currentUsername);

        ResourceStatus previousStatus = resource.getStatus();

        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        resource.setUrl(request.getUrl());
        resource.setType(request.getType());
        if (request.getStatus() != null) {
            applyStatus(resource, request.getStatus());
        }

        applyCategory(resource, request.getCategoryId());
        applyTags(resource, request.getTags());

        Resource saved = resourceRepository.save(resource);

        publishUpdated(saved, currentUsername);

        // A draft that was just published is a new post for the author's followers.
        if (previousStatus != ResourceStatus.PUBLISHED && saved.getStatus() == ResourceStatus.PUBLISHED) {
            notificationService.notifyFollowers(saved.getUser(), saved, "published a new resource", null);
        }

        return withBlocks(enrich(resourceMapper.toResponse(saved), saved, currentUsername), saved);
    }

    @Override
    @Transactional
    public void delete(Long id, String currentUsername) {
        Resource resource = findResourceOrThrow(id);
        assertOwnerOrAdmin(resource, currentUsername);
        deleteInternal(resource, currentUsername);
    }

    /** Deletes one resource together with everything that points at it. */
    private void deleteInternal(Resource resource, String actor) {
        Long id = resource.getId();

        // Reposts are resource rows too; delete their dependent data before removing the original.
        List<Resource> reposts = new ArrayList<>(resourceRepository.findAllBySharedFromId(id));
        reposts.forEach(repost -> deleteInternal(repost, actor));

        List<Media> files = new ArrayList<>(resource.getMedia());

        resource.getBlocks().clear();
        resourceRepository.flush();
        resource.getTags().clear();
        resourceRepository.nullifySharedFrom(id);

        resourceViewRepository.deleteByResourceId(id);
        feedSeenRepository.deleteByResourceId(id);
        notificationService.deleteForResource(id);
        reactionRepository.deleteByTargetTypeAndResourceId(ReactionTargetType.COMMENT, id);
        commentRepository.deleteRepliesByResourceId(id);
        commentRepository.deleteByResourceId(id);
        reactionRepository.deleteByTargetTypeAndTargetId(ReactionTargetType.RESOURCE, id);
        bookmarkRepository.deleteByResourceId(id);
        preferenceRepository.deleteByResourceId(id);

        resourceRepository.delete(resource);
        files.forEach(mediaService::deleteFile);

        realtimePublisher.publish(RealtimeEvent.builder()
                .type(RealtimeEventType.RESOURCE_DELETED)
                .resourceId(id)
                .actor(actor)
                .build());
    }

    // ------------------------------------------------------------------ status

    @Override
    @Transactional
    public ResourceResponse changeStatus(Long id, ResourceStatus status, String currentUsername) {
        Resource resource = findResourceOrThrow(id);
        assertOwnerOrAdmin(resource, currentUsername);

        ResourceStatus previous = resource.getStatus();
        if (previous != status) {
            applyStatus(resource, status);
            Resource saved = resourceRepository.save(resource);

            publishUpdated(saved, currentUsername);

            // Publishing a draft (or restoring an archived post straight to public) is news for followers.
            if (previous != ResourceStatus.PUBLISHED && status == ResourceStatus.PUBLISHED) {
                notificationService.notifyFollowers(saved.getUser(), saved, "published a new resource", null);
            }
            resource = saved;
        }
        return withBlocks(enrich(resourceMapper.toResponse(resource), resource, currentUsername), resource);
    }

    @Override
    @Transactional
    public PageResponse<ResourceResponse> getMine(ResourceStatus status, int page, int size, String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by("updatedAt").descending());
        Page<Resource> resources = resourceRepository.findByUserIdAndStatus(user.getId(), status, pageable);

        return PageResponse.from(resources.map(r -> enrich(resourceMapper.toResponse(r), r, currentUsername)));
    }

    @Override
    @Transactional
    public Map<String, Long> countMine(String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("published", resourceRepository.countByUserIdAndStatus(user.getId(), ResourceStatus.PUBLISHED));
        counts.put("draft", resourceRepository.countByUserIdAndStatus(user.getId(), ResourceStatus.DRAFT));
        counts.put("archived", resourceRepository.countByUserIdAndStatus(user.getId(), ResourceStatus.ARCHIVED));
        return counts;
    }

    // -------------------------------------------------- automatic clean-up of archive

    @Override
    @Transactional
    public int backfillArchivedAt() {
        return resourceRepository.backfillArchivedAt(LocalDateTime.now());
    }

    @Override
    @Transactional
    public List<Long> findExpiredArchivedIds(int retentionDays) {
        return resourceRepository.findExpiredArchivedIds(LocalDateTime.now().minusDays(retentionDays));
    }

    @Override
    @Transactional
    public void purge(Long id) {
        Resource resource = resourceRepository.findById(id).orElse(null);
        // Re-check: the author may have restored it a moment ago.
        if (resource == null || resource.getStatus() != ResourceStatus.ARCHIVED) {
            return;
        }
        deleteInternal(resource, "system");
    }

    // ------------------------------------------------------------ content blocks

    @Override
    @Transactional
    public ResourceResponse updateContent(Long id, List<BlockRequest> blocks, List<MultipartFile> files,
                                          String currentUsername) {
        Resource resource = findResourceOrThrow(id);
        assertOwnerOrAdmin(resource, currentUsername);
        User uploader = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));

        applyBlocks(resource, blocks, files, uploader);
        Resource saved = resourceRepository.save(resource);

        publishUpdated(saved, currentUsername);

        return withBlocks(enrich(resourceMapper.toResponse(saved), saved, currentUsername), saved);
    }

    /**
     * Rebuilds the ordered body. Existing files are referenced by id, new ones by their index in the
     * upload list; every attachment that no block uses any more is removed (row + file on disk).
     */
    private void applyBlocks(Resource resource, List<BlockRequest> requests, List<MultipartFile> files, User uploader) {
        List<BlockRequest> wanted = requests == null ? List.of() : requests;
        if (wanted.size() > MAX_BLOCKS) {
            throw new IllegalArgumentException("A resource can have at most " + MAX_BLOCKS + " content blocks");
        }

        // Load the collections BEFORE anything is inserted, so they cannot contain a half-saved state.
        List<Media> existing = new ArrayList<>(resource.getMedia());
        resource.getBlocks().clear();
        resourceRepository.flush();

        Map<Integer, Media> uploaded = new HashMap<>();
        java.util.Set<Long> used = new HashSet<>();
        int order = 0;

        for (BlockRequest request : wanted) {
            if (request.getType() == null) {
                throw new IllegalArgumentException("Every block needs a type (TEXT or MEDIA)");
            }

            ResourceBlock block = ResourceBlock.builder()
                    .resource(resource)
                    .type(request.getType())
                    .sortOrder(order)
                    .build();

            if (request.getType() == BlockType.TEXT) {
                String text = request.getText() == null ? "" : request.getText().strip();
                if (text.isEmpty()) {
                    continue; // an empty paragraph is just dropped
                }
                if (text.length() > MAX_BLOCK_TEXT) {
                    throw new IllegalArgumentException("A text block can have at most " + MAX_BLOCK_TEXT + " characters");
                }
                block.setText(text);
            } else {
                Media media;
                if (request.getFileIndex() != null) {
                    int index = request.getFileIndex();
                    if (files == null || index < 0 || index >= files.size()
                            || files.get(index) == null || files.get(index).isEmpty()) {
                        throw new IllegalArgumentException("A block refers to a file that was not uploaded");
                    }
                    media = uploaded.get(index);
                    if (media == null) {
                        media = mediaService.attachToResource(resource, files.get(index), uploader);
                        uploaded.put(index, media);
                        resource.getMedia().add(media);
                    }
                } else if (request.getMediaId() != null) {
                    Long mediaId = request.getMediaId();
                    media = existing.stream()
                            .filter(m -> m.getId().equals(mediaId))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Attachment does not belong to this resource"));
                } else {
                    throw new IllegalArgumentException("A file block needs mediaId or fileIndex");
                }
                block.setMedia(media);
                used.add(media.getId());
            }

            resource.getBlocks().add(block);
            order++;
        }

        for (Media old : existing) {
            if (!used.contains(old.getId())) {
                resource.getMedia().remove(old);
                mediaService.deleteFile(old);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Single place that changes the status, so archivedAt can never get out of sync. */
    private void applyStatus(Resource resource, ResourceStatus status) {
        ResourceStatus previous = resource.getStatus();
        resource.setStatus(status);
        if (status == ResourceStatus.ARCHIVED) {
            if (previous != ResourceStatus.ARCHIVED || resource.getArchivedAt() == null) {
                resource.setArchivedAt(LocalDateTime.now());
            }
        } else {
            resource.setArchivedAt(null);
        }
    }

    private boolean canManage(Resource resource, String username) {
        if (username == null) {
            return false;
        }
        return userRepository.findByUsername(username).map(user ->
                (resource.getUser() != null && resource.getUser().getId().equals(user.getId()))
                        || user.getRole() == Role.ROLE_ADMIN).orElse(false);
    }

    private ResourceResponse withBlocks(ResourceResponse response, Resource resource) {
        List<BlockResponse> blocks = new ArrayList<>();
        for (ResourceBlock block : resource.getBlocks()) {
            blocks.add(BlockResponse.builder()
                    .id(block.getId())
                    .type(block.getType())
                    .text(block.getText())
                    .media(block.getMedia() == null ? null
                            : resourceMapper.mapMedia(List.of(block.getMedia())).get(0))
                    .build());
        }
        response.setBlocks(blocks);
        return response;
    }

    private ResourceResponse enrich(ResourceResponse response, Resource resource) {
        return enrich(response, resource, null);
    }

    private ResourceResponse enrich(ResourceResponse response, Resource resource, String currentUsername) {
        response.setViewCount(resource.getViewCount());
        if (resource.getStatus() == ResourceStatus.ARCHIVED && resource.getArchivedAt() != null) {
            response.setPurgeAt(resource.getArchivedAt().plusDays(archiveRetentionDays));
        }
        response.setCommentCount(commentRepository.countByResourceId(resource.getId()));
        response.setReactionCount(reactionRepository.countByTargetTypeAndTargetId(ReactionTargetType.RESOURCE, resource.getId()));

        if (currentUsername != null) {
            userRepository.findByUsername(currentUsername).ifPresent(user -> {
                reactionRepository.findByUserIdAndTargetTypeAndTargetId(
                                user.getId(), ReactionTargetType.RESOURCE, resource.getId())
                        .map(com.setec.forum.model.Reaction::getType)
                        .ifPresent(response::setMyReaction);

                preferenceRepository.findByUserIdAndResourceId(user.getId(), resource.getId())
                        .ifPresent(preference -> response.setMyPreference(preference.getType()));

                boolean isOwner = resource.getUser() != null && resource.getUser().getId().equals(user.getId());
                boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;
                response.setCanEdit(isOwner || isAdmin);
                response.setCanDelete(isOwner || isAdmin);
            });
        }
        return response;
    }

    /**
     * Pushes an edit to everybody. The snapshot is built WITHOUT a viewer so it never leaks or
     * overwrites per-viewer fields (myReaction, bookmarked, canEdit...). A resource that is no longer
     * PUBLISHED is announced as "unpublished" instead, so public lists can drop it without anyone
     * receiving the draft's content.
     */
    private void publishUpdated(Resource saved, String actorUsername) {
        if (saved.getStatus() == ResourceStatus.PUBLISHED) {
            realtimePublisher.publish(RealtimeEvent.builder()
                    .type(RealtimeEventType.RESOURCE_UPDATED)
                    .resourceId(saved.getId())
                    .actor(actorUsername)
                    .resource(withBlocks(enrich(resourceMapper.toResponse(saved), saved, null), saved))
                    .build());
        } else {
            realtimePublisher.publish(RealtimeEvent.builder()
                    .type(RealtimeEventType.RESOURCE_UNPUBLISHED)
                    .resourceId(saved.getId())
                    .actor(actorUsername)
                    .build());
        }
    }

    private Resource findResourceOrThrow(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Resource", id));
    }

    private User resolveAuthor(Long requestedUserId, String currentUsername) {
        if (requestedUserId != null) {
            return userRepository.findById(requestedUserId)
                    .orElseThrow(() -> ResourceNotFoundException.of("User", requestedUserId));
        }
        return userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }

    private void applyCategory(Resource resource, Long categoryId) {
        if (categoryId == null) {
            resource.setCategory(null);
            return;
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", categoryId));
        resource.setCategory(category);
    }

    private void applyTags(Resource resource, List<String> tagNames) {
        if (tagNames == null) {
            resource.setTags(new HashSet<>());
            return;
        }

        Set<Tag> tags = new HashSet<>();
        for (String rawName : tagNames) {
            if (!StringUtils.hasText(rawName)) {
                continue;
            }
            String normalized = rawName.trim();
            Tag tag = tagRepository.findByNameIgnoreCase(normalized)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(normalized).build()));
            tags.add(tag);
        }
        resource.setTags(tags);
    }

    private void assertOwnerOrAdmin(Resource resource, String currentUsername) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));

        boolean isOwner = resource.getUser() != null && resource.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You do not have permission to modify this resource");
        }
    }
}