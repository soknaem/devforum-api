package com.setec.forum.feature.forum.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.setec.forum.base.PageResponse;
import com.setec.forum.exception.ResourceNotFoundException;
import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.forum.ForumReplyRepository;
import com.setec.forum.feature.forum.ForumTopicRepository;
import com.setec.forum.feature.forum.dto.ForumCategoryResponse;
import com.setec.forum.feature.forum.dto.ForumContentBlock;
import com.setec.forum.feature.forum.dto.ForumReplyRequest;
import com.setec.forum.feature.forum.dto.ForumReplyResponse;
import com.setec.forum.feature.forum.dto.ForumTopicRequest;
import com.setec.forum.feature.forum.dto.ForumTopicResponse;
import com.setec.forum.feature.forum.service.ForumService;
import com.setec.forum.feature.media.MediaRepository;
import com.setec.forum.feature.notification.service.NotificationService;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.feature.share.dto.request.ShareRequest;
import com.setec.forum.model.ForumBlockType;
import com.setec.forum.model.ForumCategory;
import com.setec.forum.model.ForumReply;
import com.setec.forum.model.ForumTopic;
import com.setec.forum.model.Media;
import com.setec.forum.model.MediaType;
import com.setec.forum.model.NotificationType;
import com.setec.forum.model.Role;
import com.setec.forum.model.User;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForumServiceImpl implements ForumService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_TOTAL_TEXT = 50000;

    private final ForumTopicRepository topicRepository;
    private final ForumReplyRepository replyRepository;
    private final UserRepository userRepository;
    private final MediaRepository mediaRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    public List<ForumCategoryResponse> getCategories() {
        return List.of(ForumCategory.values()).stream()
                .map(category -> ForumCategoryResponse.builder()
                        .key(category.getKey())
                        .label(category.getLabel())
                        .description(category.getDescription())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<ForumTopicResponse> getTopics(String categoryKey, String sortBy, int page, int size, String username) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), boundedSize(size), topicSort(sortBy));
        Page<ForumTopic> topics;
        if (categoryKey == null || categoryKey.isBlank()) {
            topics = topicRepository.findAll(pageable);
        } else {
            topics = topicRepository.findByCategoryKey(ForumCategory.fromKey(categoryKey).getKey(), pageable);
        }
        User viewer = findViewer(username);
        return PageResponse.from(topics.map(topic -> toTopicResponse(topic, false, viewer)));
    }

    @Transactional
    @Override
    public ForumTopicResponse getTopic(Long topicId, String username) {
        if (topicRepository.incrementViewCount(topicId) == 0) {
            throw ResourceNotFoundException.of("Forum topic", topicId);
        }
        return toTopicResponse(requireTopic(topicId), true, findViewer(username));
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<ForumReplyResponse> getReplies(Long topicId, int page, int size) {
        if (!topicRepository.existsById(topicId)) {
            throw ResourceNotFoundException.of("Forum topic", topicId);
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), boundedSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(replyRepository.findByTopicId(topicId, pageable)
                .map(this::toReplyResponse));
    }

    @Transactional
    @Override
    public ForumTopicResponse createTopic(ForumTopicRequest request, String username) {
        User author = requireUser(username);
        ForumCategory category = ForumCategory.fromKey(request.getCategoryKey());
        List<ForumContentBlock> blocks = prepareBlocks(request.getBlocks(), author);
        String normalizedTitle = request.getTitle().trim();
        if (normalizedTitle.length() < 8) {
            throw new IllegalArgumentException("Use a title with at least 8 characters");
        }
        ForumTopic topic = ForumTopic.builder()
                .title(normalizedTitle)
                .categoryKey(category.getKey())
                .summary(summarize(blocks))
                .bodyJson(writeBlocks(blocks))
                .author(author)
                .build();
        return toTopicResponse(topicRepository.save(topic), true, author);
    }

    @Transactional
    @Override
    public ForumReplyResponse createReply(Long topicId, ForumReplyRequest request, String username) {
        User author = requireUser(username);
        ForumTopic topic = requireTopic(topicId);
        List<ForumContentBlock> blocks = prepareBlocks(request.getBlocks(), author);
        ForumReply reply = ForumReply.builder()
                .topic(topic)
                .author(author)
                .contentJson(writeBlocks(blocks))
                .build();
        ForumReply saved = replyRepository.save(reply);
        topicRepository.incrementReplyCount(topicId);
        return toReplyResponse(saved);
    }

    /**
     * A share is a new topic that points back at the original through {@code sharedFrom}, so it shows
     * up in the forum list like any other topic. The caption is the share's own content; the original
     * is read live, so the share always shows the up-to-date discussion.
     */
    @Transactional
    @Override
    public ForumTopicResponse shareTopic(Long topicId, ShareRequest request, String username) {
        User sharer = requireUser(username);
        ForumTopic original = requireTopic(topicId);
        // Sharing a share re-shares the ORIGINAL discussion, so shares never nest.
        if (original.getSharedFrom() != null) {
            original = original.getSharedFrom();
        }

        String caption = request != null ? trimToNull(request.getComment(), 500) : null;
        List<ForumContentBlock> blocks = caption == null
                ? List.of()
                : List.of(ForumContentBlock.builder().type(ForumBlockType.TEXT).text(caption).build());

        ForumTopic share = ForumTopic.builder()
                .title(original.getTitle())
                .categoryKey(original.getCategoryKey())
                .summary(caption == null ? null : (caption.length() > 260 ? caption.substring(0, 259) + "…" : caption))
                .bodyJson(writeBlocks(blocks))
                .author(sharer)
                .sharedFrom(original)
                .build();
        ForumTopic saved = topicRepository.save(share);

        notificationService.notify(original.getAuthor(), sharer, NotificationType.SHARE,
                "FORUM_TOPIC", original.getId(),
                sharer.getUsername() + " shared your discussion \"" + original.getTitle() + "\"");

        return toTopicResponse(saved, true, sharer);
    }

    @Transactional
    @Override
    public void deleteTopic(Long topicId, String username) {
        User user = requireUser(username);
        ForumTopic topic = requireTopic(topicId);
        if (!canModify(topic, user)) {
            throw new UnauthorizedException("You do not have permission to delete this discussion");
        }
        // Shares embed this discussion, so they go with it (each with its own replies first).
        for (ForumTopic share : topicRepository.findBySharedFromId(topicId)) {
            replyRepository.deleteByTopicId(share.getId());
            topicRepository.delete(share);
        }
        replyRepository.deleteByTopicId(topicId);
        topicRepository.delete(topic);
    }

    private Sort topicSort(String sortBy) {
        if ("popular".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Order.desc("replyCount"), Sort.Order.desc("id"));
        }
        return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    }

    private int boundedSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private List<ForumContentBlock> prepareBlocks(List<ForumContentBlock> requested, User author) {
        if (requested == null || requested.isEmpty() || requested.size() > 40) {
            throw new IllegalArgumentException("Add between 1 and 40 content blocks");
        }

        int totalText = 0;
        List<ForumContentBlock> prepared = new ArrayList<>();
        for (ForumContentBlock block : requested) {
            if (block == null || block.getType() == null) {
                throw new IllegalArgumentException("Every content block must have a type");
            }
            switch (block.getType()) {
                case TEXT -> {
                    String text = requiredText(block.getText(), 10000, "Text blocks cannot be empty or longer than 10000 characters");
                    totalText += text.length();
                    prepared.add(ForumContentBlock.builder().type(ForumBlockType.TEXT).text(text).build());
                }
                case CODE -> {
                    String text = requiredText(block.getText(), 30000, "Code blocks cannot be empty or longer than 30000 characters");
                    totalText += text.length();
                    prepared.add(ForumContentBlock.builder()
                            .type(ForumBlockType.CODE)
                            .text(text)
                            .language(trimToNull(block.getLanguage(), 40))
                            .build());
                }
                case LINK -> {
                    String url = safeHttpUrl(block.getUrl());
                    if (url == null) {
                        throw new IllegalArgumentException("Links must use a valid http or https URL");
                    }
                    prepared.add(ForumContentBlock.builder()
                            .type(ForumBlockType.LINK)
                            .url(url)
                            .title(trimToNull(block.getTitle(), 200))
                            .build());
                }
                case IMAGE, FILE -> prepared.add(prepareMediaBlock(block, author));
            }
            if (totalText > MAX_TOTAL_TEXT) {
                throw new IllegalArgumentException("The combined text and code content is too long");
            }
        }
        if (prepared.isEmpty()) {
            throw new IllegalArgumentException("Add at least one content block");
        }
        return prepared;
    }

    private ForumContentBlock prepareMediaBlock(ForumContentBlock block, User author) {
        if (block.getMediaId() == null) {
            throw new IllegalArgumentException("Upload the attachment before adding it to a discussion");
        }
        Media media = mediaRepository.findById(block.getMediaId())
                .orElseThrow(() -> ResourceNotFoundException.of("Media", block.getMediaId()));
        if (media.getUploadedBy() == null || !Objects.equals(media.getUploadedBy().getId(), author.getId())) {
            throw new UnauthorizedException("You can only attach files that you uploaded");
        }
        if (block.getType() == ForumBlockType.IMAGE && media.getMediaType() != MediaType.IMAGE) {
            throw new IllegalArgumentException("Image blocks must use an uploaded image");
        }
        if (block.getType() == ForumBlockType.FILE && media.getMediaType() == MediaType.IMAGE) {
            throw new IllegalArgumentException("Use an image block for image attachments");
        }
        return ForumContentBlock.builder()
                .type(block.getType())
                .mediaId(media.getId())
                .url(media.getUrl())
                .fileName(media.getFileName())
                .contentType(media.getContentType())
                .alt(block.getType() == ForumBlockType.IMAGE ? trimToNull(block.getAlt(), 200) : null)
                .title(block.getType() == ForumBlockType.FILE ? media.getFileName() : null)
                .build();
    }

    private String requiredText(String value, int maxLength, String message) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String trimToNull(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String safeHttpUrl(String value) {
        if (value == null || value.isBlank() || value.length() > 2048) return null;
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null || scheme == null) return null;
            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            if (!normalizedScheme.equals("http") && !normalizedScheme.equals("https")) return null;
            return uri.toASCIIString();
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private String writeBlocks(List<ForumContentBlock> blocks) {
        try {
            return objectMapper.writeValueAsString(blocks);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not save discussion content", ex);
        }
    }

    private List<ForumContentBlock> readBlocks(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ForumContentBlock>>() { });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not read discussion content", ex);
        }
    }

    private ForumTopic requireTopic(Long topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> ResourceNotFoundException.of("Forum topic", topicId));
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }

    /** The signed-in user, or null (e.g. an anonymous request). */
    private User findViewer(String username) {
        return username == null ? null : userRepository.findByUsername(username).orElse(null);
    }

    private boolean canModify(ForumTopic topic, User viewer) {
        if (viewer == null) return false;
        boolean isOwner = topic.getAuthor() != null && Objects.equals(topic.getAuthor().getId(), viewer.getId());
        return isOwner || viewer.getRole() == Role.ROLE_ADMIN;
    }

    /** First image of a topic, or null. Never throws: a list must not fail because of one odd row. */
    private String coverImageUrl(ForumTopic topic) {
        try {
            for (ForumContentBlock block : readBlocks(topic.getBodyJson())) {
                if (block.getType() == ForumBlockType.IMAGE && block.getUrl() != null && !block.getUrl().isBlank()) {
                    return block.getUrl();
                }
            }
        } catch (IllegalStateException ignored) {
            // unreadable content: no cover
        }
        return null;
    }

    private ForumTopicResponse toTopicResponse(ForumTopic topic, boolean includeBlocks, User viewer) {
        return toTopicResponse(topic, includeBlocks, viewer, true);
    }

    private ForumTopicResponse toTopicResponse(ForumTopic topic, boolean includeBlocks, User viewer, boolean withShared) {
        ForumCategory category = ForumCategory.fromKey(topic.getCategoryKey());
        ForumTopic original = withShared ? topic.getSharedFrom() : null;
        boolean modifiable = canModify(topic, viewer);
        return ForumTopicResponse.builder()
                .id(topic.getId())
                .title(topic.getTitle())
                .categoryKey(category.getKey())
                .categoryLabel(category.getLabel())
                .summary(topic.getSummary())
                .blocks(includeBlocks ? readBlocks(topic.getBodyJson()) : List.of())
                .authorId(topic.getAuthor().getId())
                .authorUsername(topic.getAuthor().getUsername())
                .authorAvatarUrl(topic.getAuthor().getProfile() != null ? topic.getAuthor().getProfile().getAvatarUrl() : null)
                .replyCount(topic.getReplyCount())
                .viewCount(topic.getViewCount())
                .coverImageUrl(coverImageUrl(topic))
                .sharedFrom(original != null ? toTopicResponse(original, includeBlocks, viewer, false) : null)
                .canEdit(modifiable)
                .canDelete(modifiable)
                .createdAt(topic.getCreatedAt())
                .updatedAt(topic.getUpdatedAt())
                .build();
    }

    private ForumReplyResponse toReplyResponse(ForumReply reply) {
        User author = reply.getAuthor();
        return ForumReplyResponse.builder()
                .id(reply.getId())
                .topicId(reply.getTopic().getId())
                .blocks(readBlocks(reply.getContentJson()))
                .authorId(author.getId())
                .authorUsername(author.getUsername())
                .authorAvatarUrl(author.getProfile() != null ? author.getProfile().getAvatarUrl() : null)
                .edited(reply.isEdited())
                .createdAt(reply.getCreatedAt())
                .updatedAt(reply.getUpdatedAt())
                .build();
    }

    private String summarize(List<ForumContentBlock> blocks) {
        return blocks.stream()
                .map(block -> switch (block.getType()) {
                    case TEXT -> block.getText();
                    case CODE -> "Code sample: " + block.getText();
                    case LINK -> block.getTitle() != null ? block.getTitle() : block.getUrl();
                    case IMAGE -> "Image attachment";
                    case FILE -> block.getFileName() != null ? block.getFileName() : "File attachment";
                })
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .findFirst()
                .map(value -> value.length() > 260 ? value.substring(0, 259) + "…" : value)
                .orElse("Open this discussion to see the shared context.");
    }
}