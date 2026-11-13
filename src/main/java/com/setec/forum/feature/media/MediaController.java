package com.setec.forum.feature.media;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.feature.media.dto.response.MediaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import com.setec.forum.feature.media.service.MediaService;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    /**
     * Generic standalone upload - used for things like a user avatar/cover that aren't tied to a
     * Resource. `folder` lets the caller organize storage (e.g. "avatars", "covers", "misc").
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<MediaResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "misc") String folder,
            Authentication authentication) {
        MediaResponse response = mediaService.uploadStandalone(file, folder, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("File uploaded successfully", response));
    }

    /** Attach an uploaded file (book PDF, code archive, screenshot, ...) to an existing resource. */
    @PostMapping(value = "/resources/{resourceId}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<MediaResponse>> uploadForResource(
            @PathVariable Long resourceId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        MediaResponse response = mediaService.uploadForResource(resourceId, file, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Media attached to resource", response));
    }

    @GetMapping("/resources/{resourceId}")
    public ApiResponse<List<MediaResponse>> getForResource(@PathVariable Long resourceId) {
        return ApiResponse.success(mediaService.getForResource(resourceId));
    }

    @DeleteMapping("/{mediaId}")
    public ApiResponse<Void> delete(@PathVariable Long mediaId, Authentication authentication) {
        mediaService.delete(mediaId, authentication.getName());
        return ApiResponse.success("Media deleted", null);
    }
}
