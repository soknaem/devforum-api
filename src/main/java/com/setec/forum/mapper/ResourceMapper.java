package com.setec.forum.mapper;

import com.setec.forum.feature.media.dto.response.MediaResponse;
import com.setec.forum.feature.resource.dto.request.ResourceRequest;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.model.Media;
import com.setec.forum.model.Resource;
import com.setec.forum.model.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ResourceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "media", ignore = true)
    @Mapping(target = "sharedFrom", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "blocks", ignore = true)
    @Mapping(target = "archivedAt", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "updatedAt", ignore = true)
    Resource toEntity(ResourceRequest request);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "authorId", source = "user.id")
    @Mapping(target = "authorUsername", source = "user.username")
    @Mapping(target = "tags", expression = "java(mapTagNames(resource.getTags()))")
    @Mapping(target = "media", expression = "java(mapMedia(resource.getMedia()))")
    @Mapping(target = "commentCount", ignore = true)
    @Mapping(target = "reactionCount", ignore = true)
    // Blocks are attached explicitly (single-resource endpoints only) to avoid loading them for every list row.
    @Mapping(target = "blocks", ignore = true)
    @Mapping(target = "purgeAt", ignore = true)
    @Mapping(target = "sharedFromId", source = "sharedFrom.id")
    @Mapping(target = "sharedFromTitle", source = "sharedFrom.title")
    @Mapping(target = "sharedFromAuthorUsername", source = "sharedFrom.user.username")
    @Mapping(target = "sharedFrom", expression = "java(mapOriginal(resource.getSharedFrom()))")
    ResourceResponse toResponse(Resource resource);

    /**
     * Flat (non-recursive) copy of the original post that a share points at. Written by hand instead
     * of letting MapStruct recurse, so a share never drags a whole chain of shares into one response.
     */
    default ResourceResponse mapOriginal(Resource original) {
        if (original == null) {
            return null;
        }
        // Older shares may point at another share - always show the root post.
        while (original.getSharedFrom() != null) {
            original = original.getSharedFrom();
        }
        // The author moved the original to draft / archive: a share must not keep showing it.
        if (original.getStatus() != com.setec.forum.model.ResourceStatus.PUBLISHED) {
            return ResourceResponse.builder()
                    .id(original.getId())
                    .status(original.getStatus())
                    .title("This post is no longer available")
                    .createdAt(original.getCreatedAt())
                    .build();
        }
        return ResourceResponse.builder()
                .id(original.getId())
                .title(original.getTitle())
                .description(original.getDescription())
                .url(original.getUrl())
                .type(original.getType())
                .status(original.getStatus())
                .categoryId(original.getCategory() != null ? original.getCategory().getId() : null)
                .categoryName(original.getCategory() != null ? original.getCategory().getName() : null)
                .authorId(original.getUser() != null ? original.getUser().getId() : null)
                .authorUsername(original.getUser() != null ? original.getUser().getUsername() : null)
                .tags(mapTagNames(original.getTags()))
                .media(mapMedia(original.getMedia()))
                .viewCount(original.getViewCount())
                .createdAt(original.getCreatedAt())
                .updatedAt(original.getUpdatedAt())
                .build();
    }

    default List<String> mapTagNames(Set<Tag> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream().map(Tag::getName).sorted().collect(Collectors.toList());
    }

    default List<MediaResponse> mapMedia(List<Media> media) {
        if (media == null) {
            return List.of();
        }
        return media.stream()
                .map(m -> MediaResponse.builder()
                        .id(m.getId())
                        .fileName(m.getFileName())
                        .url(m.getUrl())
                        .contentType(m.getContentType())
                        .sizeBytes(m.getSizeBytes())
                        .mediaType(m.getMediaType())
                        .resourceId(m.getResource() != null ? m.getResource().getId() : null)
                        .build())
                .collect(Collectors.toList());
    }
}
