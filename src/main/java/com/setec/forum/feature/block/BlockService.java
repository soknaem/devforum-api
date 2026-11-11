package com.setec.forum.feature.block;

import com.setec.forum.exception.ResourceNotFoundException;
import com.setec.forum.feature.follow.FollowRepository;
import com.setec.forum.feature.follow.dto.response.UserSummaryResponse;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.model.User;
import com.setec.forum.model.UserBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Blocking works in both directions: once A blocks B, A no longer sees B's posts, comments or
 * notifications, and B can no longer follow, comment on or notify A. Existing follows between the
 * two are removed.
 */
@Service
@RequiredArgsConstructor
public class BlockService {

    private final UserBlockRepository blockRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Transactional
    public void block(String targetUsername, String currentUsername) {
        User me = requireUser(currentUsername);
        User target = requireUser(targetUsername);

        if (me.getId().equals(target.getId())) {
            throw new IllegalArgumentException("You cannot block yourself");
        }
        if (blockRepository.existsByBlockerIdAndBlockedId(me.getId(), target.getId())) {
            return; // idempotent
        }

        blockRepository.save(UserBlock.builder().blocker(me).blocked(target).build());

        // Nobody keeps following the other side after a block.
        followRepository.findByFollowerIdAndFollowingId(me.getId(), target.getId())
                .ifPresent(followRepository::delete);
        followRepository.findByFollowerIdAndFollowingId(target.getId(), me.getId())
                .ifPresent(followRepository::delete);
    }

    @Transactional
    public void unblock(String targetUsername, String currentUsername) {
        User me = requireUser(currentUsername);
        User target = requireUser(targetUsername);
        blockRepository.findByBlockerIdAndBlockedId(me.getId(), target.getId())
                .ifPresent(blockRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean isBlockedByMe(String currentUsername, String targetUsername) {
        User me = requireUser(currentUsername);
        User target = requireUser(targetUsername);
        return blockRepository.existsByBlockerIdAndBlockedId(me.getId(), target.getId());
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listBlocked(String currentUsername) {
        User me = requireUser(currentUsername);
        return blockRepository.findByBlockerIdOrderByCreatedAtDesc(me.getId()).stream()
                .map(UserBlock::getBlocked)
                .map(user -> UserSummaryResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getProfile() != null ? user.getProfile().getFullName() : null)
                        .avatarUrl(user.getProfile() != null ? user.getProfile().getAvatarUrl() : null)
                        .build())
                .toList();
    }

    /** Everybody the user has blocked PLUS everybody who blocked the user - none of them is shown. */
    @Transactional(readOnly = true)
    public Set<Long> hiddenUserIds(Long userId) {
        Set<Long> ids = new HashSet<>();
        if (userId == null) {
            return ids;
        }
        ids.addAll(blockRepository.findBlockedIds(userId));
        ids.addAll(blockRepository.findBlockerIds(userId));
        return ids;
    }

    @Transactional(readOnly = true)
    public boolean isBlockedBetween(Long userA, Long userB) {
        if (userA == null || userB == null) {
            return false;
        }
        return blockRepository.existsByBlockerIdAndBlockedId(userA, userB)
                || blockRepository.existsByBlockerIdAndBlockedId(userB, userA);
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> ResourceNotFoundException.of("User", username));
    }
}
