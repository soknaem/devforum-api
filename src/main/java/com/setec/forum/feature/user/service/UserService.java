package com.setec.forum.feature.user.service;

import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.feature.user.*;
import com.setec.forum.feature.user.dto.request.UpdateProfileRequest;
import com.setec.forum.feature.user.dto.response.UserProfileResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserProfileResponse getProfile(String username);
    UserProfileResponse getProfile(String username, String viewerUsername);
    UserProfileResponse updateProfile(String currentUsername, UpdateProfileRequest request);
    UserProfileResponse updateAvatar(String currentUsername, MultipartFile file);
    UserProfileResponse updateCover(String currentUsername, MultipartFile file);
    List<ResourceResponse> getResourcesByUser(String username, int page, int size);
}
