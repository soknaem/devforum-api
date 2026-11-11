package com.setec.forum.feature.preference;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.feature.preference.dto.request.PreferenceRequest;
import com.setec.forum.feature.preference.dto.response.PreferenceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backs the "Interested" / "Not interested" items of a post's "..." menu.
 *
 * PUT    /api/v1/resources/{id}/preference   {"type":"INTERESTED" | "NOT_INTERESTED"}
 * DELETE /api/v1/resources/{id}/preference   (undo)
 */
@RestController
@RequiredArgsConstructor
public class ResourcePreferenceController {

    private final ResourcePreferenceService preferenceService;

    @PutMapping("/api/v1/resources/{resourceId}/preference")
    public ApiResponse<PreferenceResponse> set(
            @PathVariable Long resourceId,
            @Valid @RequestBody PreferenceRequest request,
            Authentication authentication) {
        return ApiResponse.success("Preference saved",
                preferenceService.set(resourceId, request.getType(), authentication.getName()));
    }

    @DeleteMapping("/api/v1/resources/{resourceId}/preference")
    public ApiResponse<PreferenceResponse> clear(
            @PathVariable Long resourceId,
            Authentication authentication) {
        return ApiResponse.success("Preference removed",
                preferenceService.clear(resourceId, authentication.getName()));
    }
}
