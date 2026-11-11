package com.setec.forum.feature.user;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.feature.user.dto.request.UpdateProfileRequest;
import com.setec.forum.feature.user.dto.response.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ResourceViewService resourceViewService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(Authentication authentication) {
        return ApiResponse.success(userService.getProfile(authentication.getName()));
    }

    @PutMapping("/me/profile")
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
        return ApiResponse.success("Profile updated", userService.updateProfile(authentication.getName(), request));
    }

    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    public ApiResponse<UserProfileResponse> updateAvatar(
            @RequestParam("file") MultipartFile file, Authentication authentication) {
        return ApiResponse.success("Avatar updated", userService.updateAvatar(authentication.getName(), file));
    }

    @PostMapping(value = "/me/cover", consumes = "multipart/form-data")
    public ApiResponse<UserProfileResponse> updateCover(
            @RequestParam("file") MultipartFile file, Authentication authentication) {
        return ApiResponse.success("Cover photo updated", userService.updateCover(authentication.getName(), file));
    }

    @GetMapping("/me/history")
    public ApiResponse<PageResponse<ResourceResponse>> getMyHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(resourceViewService.getHistory(authentication.getName(), page, size));
    }

    @GetMapping("/{username}")
    public ApiResponse<UserProfileResponse> getProfile(@PathVariable String username, Authentication authentication) {
        boolean authenticated = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        String viewer = authenticated ? authentication.getName() : null;
        return ApiResponse.success(userService.getProfile(username, viewer));
    }

    @GetMapping("/{username}/resources")
    public ApiResponse<List<ResourceResponse>> getResources(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(userService.getResourcesByUser(username, page, size));
    }
}
