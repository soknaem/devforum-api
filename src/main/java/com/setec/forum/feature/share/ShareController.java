package com.setec.forum.feature.share;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.feature.share.dto.request.ShareRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @PostMapping("/api/v1/resources/{id}/share")
    public ResponseEntity<ApiResponse<ResourceResponse>> share(
            @PathVariable Long id,
            @RequestBody(required = false) ShareRequest request,
            Authentication authentication) {
        ResourceResponse response = shareService.share(id, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Resource shared", response));
    }
}
