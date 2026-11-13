package com.setec.forum.feature.media.service.impl;

import com.setec.forum.exception.ResourceNotFoundException;
import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.media.*;
import com.setec.forum.feature.media.dto.response.MediaResponse;
import com.setec.forum.feature.media.service.MediaService;
import com.setec.forum.feature.resource.ResourceBlockRepository;
import com.setec.forum.feature.resource.ResourceRepository;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.model.Media;
import com.setec.forum.model.MediaType;
import com.setec.forum.model.Resource;
import com.setec.forum.model.Role;
import com.setec.forum.model.User;
import com.setec.forum.storage.StorageService;
import com.setec.forum.storage.StoredFile;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private static final List<String> IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");
    private static final List<String> DOCUMENT_EXTENSIONS = List.of("pdf", "epub", "md", "txt");
    private static final List<String> ARCHIVE_EXTENSIONS = List.of("zip", "tar", "gz", "rar", "7z");

    private final StorageService storageService;
    private final MediaRepository mediaRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ResourceBlockRepository resourceBlockRepository;

    /** Standalone upload (e.g. a user avatar/cover) not attached to any Resource. */
    @Transactional
    @Override
    public MediaResponse uploadStandalone(MultipartFile file, String subFolder, String currentUsername) {
        User uploader = requireUser(currentUsername);
        Media media = persist(file, subFolder, uploader, null);
        return toResponse(media);
    }

    /** Upload and attach one file to an existing Resource. Only the resource's author (or an admin) may attach media. */
    @Transactional
    @Override
    public MediaResponse uploadForResource(Long resourceId, MultipartFile file, String currentUsername) {
        User uploader = requireUser(currentUsername);
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Resource", resourceId));

        boolean isOwner = resource.getUser().getId().equals(uploader.getId());
        boolean isAdmin = uploader.getRole() == Role.ROLE_ADMIN;
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You do not have permission to add media to this resource");
        }

        Media media = persist(file, "resources", uploader, resource);
        return toResponse(media);
    }

    /**
     * Stores a file and attaches it to a resource the caller has ALREADY authorised access to
     * (used by the block editor, which checks ownership once for the whole request).
     */
    @Transactional
    @Override
    public Media attachToResource(Resource resource, MultipartFile file, User uploader) {
        return persist(file, "resources", uploader, resource);
    }

    /** Removes the file from disk. The row itself is removed by the caller (orphanRemoval). */
    @Override
    public void deleteFile(Media media) {
        try {
            storageService.delete(media.getStorageKey());
        } catch (Exception ex) {
            // A missing file on disk must never block deleting the post.
        }
    }

    @Transactional
    @Override
    public List<MediaResponse> getForResource(Long resourceId) {
        return mediaRepository.findByResourceId(resourceId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void delete(Long mediaId, String currentUsername) {
        User currentUser = requireUser(currentUsername);
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> ResourceNotFoundException.of("Media", mediaId));

        boolean isUploader = media.getUploadedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;
        if (!isUploader && !isAdmin) {
            throw new UnauthorizedException("You do not have permission to delete this media");
        }

        storageService.delete(media.getStorageKey());
        resourceBlockRepository.deleteByMediaId(mediaId);
        mediaRepository.delete(media);
    }

    private Media persist(MultipartFile file, String subFolder, User uploader, Resource resource) {
        StoredFile stored = storageService.store(file, subFolder);
        Media media = Media.builder()
                .fileName(stored.getOriginalFileName())
                .storageKey(stored.getStorageKey())
                .url(stored.getUrl())
                .contentType(stored.getContentType())
                .sizeBytes(stored.getSizeBytes())
                .mediaType(classify(stored.getOriginalFileName()))
                .resource(resource)
                .uploadedBy(uploader)
                .build();
        return mediaRepository.save(media);
    }

    private MediaType classify(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (IMAGE_EXTENSIONS.contains(extension)) return MediaType.IMAGE;
        if (DOCUMENT_EXTENSIONS.contains(extension)) return MediaType.DOCUMENT;
        if (ARCHIVE_EXTENSIONS.contains(extension)) return MediaType.ARCHIVE;
        return MediaType.OTHER;
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }

    private MediaResponse toResponse(Media media) {
        return MediaResponse.builder()
                .id(media.getId())
                .fileName(media.getFileName())
                .url(media.getUrl())
                .contentType(media.getContentType())
                .sizeBytes(media.getSizeBytes())
                .mediaType(media.getMediaType())
                .resourceId(media.getResource() != null ? media.getResource().getId() : null)
                .build();
    }
}
