package com.setec.forum.feature.resource;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.media.service.MediaService;
import com.setec.forum.feature.resource.dto.request.BlockRequest;
import com.setec.forum.feature.resource.dto.request.ResourceRequest;
import com.setec.forum.feature.resource.dto.request.StatusRequest;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.model.ResourceStatus;
import com.setec.forum.model.ResourceType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.setec.forum.feature.resource.service.ResourceService;

@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;
    private final MediaService mediaService;
    private final ObjectMapper objectMapper;

    /** Pure JSON create - use this for LINK-type resources or anything with no file to attach. */
    @PostMapping
    public ResponseEntity<ApiResponse<ResourceResponse>> create(@Valid @RequestBody ResourceRequest request,
                                                                  Authentication authentication) {
        ResourceResponse response = resourceService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource created successfully", response));
    }

    /**
     * Create a resource AND upload its media (book/PDF, cover image, code archive, screenshots...)
     * in a single multipart request. `tags` is a comma-separated string since HTML multipart forms
     * don't carry repeated array fields cleanly across every client.
     */
    @PostMapping(value = "/with-media", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ResourceResponse>> createWithMedia(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String url,
            @RequestParam ResourceType type,
            @RequestParam(required = false) ResourceStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String tags,
            /** Optional JSON array of content blocks, e.g. [{"type":"TEXT","text":"..."},{"type":"MEDIA","fileIndex":0}] */
            @RequestParam(required = false) String blocks,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) {

        ResourceRequest request = ResourceRequest.builder()
                .title(title)
                .description(description)
                .url(url)
                .type(type)
                .status(status)
                .categoryId(categoryId)
                .tags(tags != null ? Arrays.asList(tags.split("\\s*,\\s*")) : null)
                .build();

        // Parse BEFORE creating anything, so a malformed body does not leave a half-built post behind.
        List<BlockRequest> blockRequests = parseBlocks(blocks);

        ResourceResponse response = resourceService.create(request, authentication.getName());

        if (blockRequests != null) {
            // Ordered body: text and files in the order the author arranged them.
            response = resourceService.updateContent(response.getId(), blockRequests, files, authentication.getName());
        } else if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    mediaService.uploadForResource(response.getId(), file, authentication.getName());
                }
            }
            response = resourceService.refresh(response.getId());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource created with media", response));
    }

    /** Replaces the ordered body (text + files) of a resource. multipart: `blocks` (JSON) + `files`. */
    @PostMapping(value = "/{id}/content", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ResourceResponse>> updateContent(
            @PathVariable Long id,
            @RequestParam(required = false) String blocks,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) {
        List<BlockRequest> blockRequests = parseBlocks(blocks);
        ResourceResponse response = resourceService.updateContent(
                id, blockRequests != null ? blockRequests : List.of(), files, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Content updated", response));
    }

    /** Publish / move to draft / archive / restore. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ResourceResponse>> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request,
            Authentication authentication) {
        ResourceResponse response = resourceService.changeStatus(id, request.getStatus(), authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Status changed to " + request.getStatus(), response));
    }

    /** The caller's own resources in one status - powers the Drafts and Archived pages. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<ResourceResponse>>> getMine(
            @RequestParam(defaultValue = "DRAFT") ResourceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                resourceService.getMine(status, page, size, authentication.getName())));
    }

    @GetMapping("/me/counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countMine(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(resourceService.countMine(authentication.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResourceResponse>> getById(@PathVariable Long id, Authentication authentication) {
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        String viewer = isAuthenticated ? authentication.getName() : null;
        ResourceResponse response = resourceService.getById(id, viewer);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ResourceResponse>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) ResourceStatus status,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {

        boolean signedIn = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
        PageResponse<ResourceResponse> response = resourceService.getAll(
                search, categoryId, type, status, tag, page, size, sortBy, sortDir,
                signedIn ? authentication.getName() : null);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ResourceResponse>> update(@PathVariable Long id,
                                                                  @Valid @RequestBody ResourceRequest request,
                                                                  Authentication authentication) {
        ResourceResponse response = resourceService.update(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Resource updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, Authentication authentication) {
        resourceService.delete(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Resource deleted successfully", null));
    }

    private List<BlockRequest> parseBlocks(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<BlockRequest>>() { });
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("`blocks` is not a valid JSON array of content blocks");
        }
    }
}
