package com.setec.forum.feature.follow;

import com.setec.forum.base.PageResponse;
import com.setec.forum.exception.ResourceNotFoundException;
import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.block.BlockService;
import com.setec.forum.feature.follow.dto.response.UserSummaryResponse;
import com.setec.forum.feature.notification.NotificationService;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.model.Follow;
import com.setec.forum.model.NotificationType;
import com.setec.forum.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BlockService blockService;

    @Transactional
    public void follow(String targetUsername, String currentUsername) {
        User follower = requireUser(currentUsername);
        User target = requireUser(targetUsername);

        if (follower.getId().equals(target.getId())) {
            throw new IllegalArgumentException("You cannot follow yourself");
        }
        if (blockService.isBlockedBetween(follower.getId(), target.getId())) {
            throw new IllegalArgumentException("You cannot follow this user");
        }
        if (followRepository.existsByFollowerIdAndFollowingId(follower.getId(), target.getId())) {
            return; // idempotent
        }

        followRepository.save(Follow.builder().follower(follower).following(target).build());
        notificationService.notify(target, follower, NotificationType.FOLLOW,
                "USER", follower.getId(), follower.getUsername() + " started following you");
    }

    @Transactional
    public void unfollow(String targetUsername, String currentUsername) {
        User follower = requireUser(currentUsername);
        User target = requireUser(targetUsername);
        followRepository.findByFollowerIdAndFollowingId(follower.getId(), target.getId())
                .ifPresent(followRepository::delete);
    }

    @Transactional
    public PageResponse<UserSummaryResponse> getFollowers(String username, int page, int size) {
        User target = requireUser(username);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Follow> follows = followRepository.findByFollowingId(target.getId(), pageable);
        return PageResponse.from(follows.map(f -> toSummary(f.getFollower())));
    }

    @Transactional
    public PageResponse<UserSummaryResponse> getFollowing(String username, int page, int size) {
        User source = requireUser(username);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Follow> follows = followRepository.findByFollowerId(source.getId(), pageable);
        return PageResponse.from(follows.map(f -> toSummary(f.getFollowing())));
    }


    public long followerCount(String username) {
        return followRepository.countByFollowingId(requireUser(username).getId());
    }

    public long followingCount(String username) {
        return followRepository.countByFollowerId(requireUser(username).getId());
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> ResourceNotFoundException.of("User", username));
    }

    private UserSummaryResponse toSummary(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getProfile() != null ? user.getProfile().getFullName() : null)
                .avatarUrl(user.getProfile() != null ? user.getProfile().getAvatarUrl() : null)
                .build();
    }
}
