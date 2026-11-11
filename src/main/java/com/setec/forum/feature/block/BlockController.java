package com.setec.forum.feature.block;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.follow.dto.response.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * POST   /api/v1/users/{username}/block    block
 * DELETE /api/v1/users/{username}/block    unblock
 * GET    /api/v1/users/{username}/block    {"blocked": true|false} - did I block this user?
 * GET    /api/v1/users/me/blocked          the users I blocked
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    @PostMapping("/{username}/block")
    public ApiResponse<Void> block(@PathVariable String username, Authentication authentication) {
        blockService.block(username, requireLogin(authentication));
        return ApiResponse.success("User blocked", null);
    }

    @DeleteMapping("/{username}/block")
    public ApiResponse<Void> unblock(@PathVariable String username, Authentication authentication) {
        blockService.unblock(username, requireLogin(authentication));
        return ApiResponse.success("User unblocked", null);
    }

    @GetMapping("/{username}/block")
    public ApiResponse<Map<String, Boolean>> status(@PathVariable String username, Authentication authentication) {
        return ApiResponse.success(Map.of("blocked", blockService.isBlockedByMe(requireLogin(authentication), username)));
    }

    @GetMapping("/me/blocked")
    public ApiResponse<List<UserSummaryResponse>> blocked(Authentication authentication) {
        return ApiResponse.success(blockService.listBlocked(requireLogin(authentication)));
    }

    private String requireLogin(Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedException("Login required");
        }
        return authentication.getName();
    }
}
