package com.setec.forum.feature.admin;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.admin.AdminDtos.AuditRow;
import com.setec.forum.feature.admin.AdminDtos.CommentRow;
import com.setec.forum.feature.admin.AdminDtos.ReportRow;
import com.setec.forum.feature.admin.AdminDtos.ResolveRequest;
import com.setec.forum.feature.admin.AdminDtos.RoleRequest;
import com.setec.forum.feature.admin.AdminDtos.Stats;
import com.setec.forum.feature.admin.AdminDtos.SuspendRequest;
import com.setec.forum.feature.admin.AdminDtos.UserRow;
import com.setec.forum.feature.resource.dto.request.StatusRequest;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.model.Report;
import com.setec.forum.model.ResourceStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin panel API. Every route under /api/v1/admin requires ROLE_ADMIN (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ApiResponse<Stats> stats() {
        return ApiResponse.success(adminService.stats());
    }

    // ---- users ----

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserRow>> users(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean suspended,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.users(search, role, suspended, page, size));
    }

    @PatchMapping("/users/{id}/role")
    public ApiResponse<UserRow> setRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request,
                                        Authentication authentication) {
        return ApiResponse.success("Role updated", adminService.setRole(id, request.role(), authentication.getName()));
    }

    @PostMapping("/users/{id}/suspend")
    public ApiResponse<UserRow> suspend(@PathVariable Long id, @Valid @RequestBody(required = false) SuspendRequest request,
                                        Authentication authentication) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.success("User suspended", adminService.suspend(id, reason, authentication.getName()));
    }

    @DeleteMapping("/users/{id}/suspend")
    public ApiResponse<UserRow> unsuspend(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success("User reactivated", adminService.unsuspend(id, authentication.getName()));
    }

    // ---- resources ----

    @GetMapping("/resources")
    public ApiResponse<PageResponse<ResourceResponse>> resources(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ResourceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.resources(search, status, page, size));
    }

    @PatchMapping("/resources/{id}/status")
    public ApiResponse<Void> setResourceStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request,
                                               Authentication authentication) {
        adminService.setResourceStatus(id, request.getStatus(), authentication.getName());
        return ApiResponse.success("Status changed", null);
    }

    @DeleteMapping("/resources/{id}")
    public ApiResponse<Void> deleteResource(@PathVariable Long id, Authentication authentication) {
        adminService.deleteResource(id, authentication.getName());
        return ApiResponse.success("Resource deleted", null);
    }

    // ---- comments ----

    @GetMapping("/comments")
    public ApiResponse<PageResponse<CommentRow>> comments(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.comments(search, page, size));
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Void> deleteComment(@PathVariable Long id, Authentication authentication) {
        adminService.deleteComment(id, authentication.getName());
        return ApiResponse.success("Comment deleted", null);
    }

    // ---- reports ----

    @GetMapping("/reports")
    public ApiResponse<PageResponse<ReportRow>> reports(
            @RequestParam(required = false) Report.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.reports(status, page, size));
    }

    @PatchMapping("/reports/{id}")
    public ApiResponse<ReportRow> resolveReport(@PathVariable Long id, @Valid @RequestBody ResolveRequest request,
                                                Authentication authentication) {
        return ApiResponse.success("Report updated", adminService.resolveReport(id, request, authentication.getName()));
    }

    // ---- audit log ----

    @GetMapping("/audit")
    public ApiResponse<PageResponse<AuditRow>> audit(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.success(adminService.audit(page, size));
    }
}
