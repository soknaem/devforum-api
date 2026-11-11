package com.setec.forum.feature.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    /** ROLE_USER or ROLE_ADMIN - the UI shows the admin panel link only for admins. */
    private String role;
    private String fullName;
    private String bio;
    private String avatarUrl;
    private String coverUrl;
    private String location;
    private String website;
    private long followerCount;
    private long followingCount;
    private long resourceCount;

    /** True when the authenticated viewer already follows this user (always false for anonymous viewers / own profile). */
    private boolean following;
    private LocalDateTime joinedAt;
}
