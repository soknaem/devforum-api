package com.setec.forum.feature.user;

import com.setec.forum.base.PageResponse;
import com.setec.forum.exception.ResourceNotFoundException;
import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.follow.FollowRepository;
import com.setec.forum.feature.media.MediaService;
import com.setec.forum.feature.resource.ResourceRepository;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.feature.user.dto.request.UpdateProfileRequest;
import com.setec.forum.feature.user.dto.response.UserProfileResponse;
import com.setec.forum.mapper.ResourceMapper;
import com.setec.forum.model.ResourceStatus;
import com.setec.forum.model.User;
import com.setec.forum.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper;
    private final MediaService mediaService;

    public UserProfileResponse getProfile(String username) {
        return getProfile(username, null);
    }

    /**
     * Same as {@link #getProfile(String)}, but also tells the profile whether {@code viewerUsername}
     * already follows this user, so the UI can render "Following" correctly after a page refresh.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username, String viewerUsername) {
        User user = requireUser(username);
        UserProfileResponse response = toProfileResponse(user);

        if (viewerUsername != null && !viewerUsername.equals(user.getUsername())) {
            userRepository.findByUsername(viewerUsername).ifPresent(viewer ->
                    response.setFollowing(
                            followRepository.existsByFollowerIdAndFollowingId(viewer.getId(), user.getId())));
        }
        return response;
    }

    @Transactional
    public UserProfileResponse updateProfile(String currentUsername, UpdateProfileRequest request) {
        User user = requireUser(currentUsername);
        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = UserProfile.builder().build();
            user.setProfile(profile);
        }
        if (request.getFullName() != null) profile.setFullName(request.getFullName());
        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getLocation() != null) profile.setLocation(request.getLocation());
        if (request.getWebsite() != null) profile.setWebsite(request.getWebsite());

        userRepository.save(user);
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateAvatar(String currentUsername, MultipartFile file) {
        User user = requireUser(currentUsername);
        String url = mediaService.uploadStandalone(file, "avatars", currentUsername).getUrl();
        ensureProfile(user).setAvatarUrl(url);
        userRepository.save(user);
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateCover(String currentUsername, MultipartFile file) {
        User user = requireUser(currentUsername);
        String url = mediaService.uploadStandalone(file, "covers", currentUsername).getUrl();
        ensureProfile(user).setCoverUrl(url);
        userRepository.save(user);
        return toProfileResponse(user);
    }

    @Transactional
    public List<ResourceResponse> getResourcesByUser(String username, int page, int size) {
        User user = requireUser(username);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by("createdAt").descending());
        return resourceRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), ResourceStatus.PUBLISHED, pageable)
                .stream()
                .map(resourceMapper::toResponse)
                .collect(Collectors.toList());
    }

    private UserProfile ensureProfile(User user) {
        if (user.getProfile() == null) {
            user.setProfile(UserProfile.builder().build());
        }
        return user.getProfile();
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> ResourceNotFoundException.of("User", username));
    }

    private UserProfileResponse toProfileResponse(User user) {
        UserProfile profile = user.getProfile();
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .fullName(profile != null ? profile.getFullName() : null)
                .bio(profile != null ? profile.getBio() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .coverUrl(profile != null ? profile.getCoverUrl() : null)
                .location(profile != null ? profile.getLocation() : null)
                .website(profile != null ? profile.getWebsite() : null)
                .followerCount(followRepository.countByFollowingId(user.getId()))
                .followingCount(followRepository.countByFollowerId(user.getId()))
                .resourceCount(resourceRepository.countByUserIdAndStatus(user.getId(), ResourceStatus.PUBLISHED))
                .joinedAt(user.getCreatedAt())
                .build();
    }
}
