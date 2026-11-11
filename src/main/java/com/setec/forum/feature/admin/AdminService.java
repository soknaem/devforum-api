package com.setec.forum.feature.admin;

import com.setec.forum.base.PageResponse;
import com.setec.forum.exception.ResourceNotFoundException;
import com.setec.forum.feature.admin.AdminDtos.AuditRow;
import com.setec.forum.feature.admin.AdminDtos.CommentRow;
import com.setec.forum.feature.admin.AdminDtos.DayPoint;
import com.setec.forum.feature.admin.AdminDtos.ReportRow;
import com.setec.forum.feature.admin.AdminDtos.ResolveRequest;
import com.setec.forum.feature.admin.AdminDtos.Stats;
import com.setec.forum.feature.admin.AdminDtos.UserRow;
import com.setec.forum.feature.comment.CommentRepository;
import com.setec.forum.feature.comment.CommentService;
import com.setec.forum.feature.reaction.ReactionRepository;
import com.setec.forum.feature.report.ReportRepository;
import com.setec.forum.feature.resource.ResourceRepository;
import com.setec.forum.feature.resource.ResourceService;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.feature.resource.ResourceSpecification;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.mapper.ResourceMapper;
import com.setec.forum.model.Comment;
import com.setec.forum.model.Report;
import com.setec.forum.model.Resource;
import com.setec.forum.model.ResourceStatus;
import com.setec.forum.model.Role;
import com.setec.forum.model.User;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Everything the admin panel can do. Each change is written to the audit log in the same transaction.
 * The caller (AdminController) is already restricted to ROLE_ADMIN by SecurityConfig.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private static final int DAYS = 14;

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final ReportRepository reportRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final ResourceService resourceService;
    private final CommentService commentService;
    private final ResourceMapper resourceMapper;

    // -------------------------------------------------------------- dashboard

    @Transactional(readOnly = true)
    public Stats stats() {
        LocalDateTime since = LocalDate.now().minusDays(DAYS - 1L).atStartOfDay();

        Map<LocalDate, long[]> days = new TreeMap<>();
        for (int i = 0; i < DAYS; i++) {
            days.put(LocalDate.now().minusDays(DAYS - 1L - i), new long[3]);
        }
        bucket(days, userRepository.findCreatedAtSince(since), 0);
        bucket(days, resourceRepository.findCreatedAtSince(since), 1);
        bucket(days, commentRepository.findCreatedAtSince(since), 2);

        List<DayPoint> daily = new ArrayList<>();
        days.forEach((date, counts) -> daily.add(new DayPoint(date.toString(), counts[0], counts[1], counts[2])));

        return new Stats(
                userRepository.count(),
                userRepository.countByRole(Role.ROLE_ADMIN),
                userRepository.countBySuspended(true),
                resourceRepository.countByStatus(ResourceStatus.PUBLISHED),
                resourceRepository.countByStatus(ResourceStatus.DRAFT),
                resourceRepository.countByStatus(ResourceStatus.ARCHIVED),
                commentRepository.count(),
                reactionRepository.count(),
                reportRepository.countByStatus(Report.Status.OPEN),
                daily);
    }

    private void bucket(Map<LocalDate, long[]> days, List<LocalDateTime> times, int index) {
        for (LocalDateTime time : times) {
            long[] counts = days.get(time.toLocalDate());
            if (counts != null) {
                counts[index]++;
            }
        }
    }

    // ------------------------------------------------------------------ users

    @Transactional(readOnly = true)
    public PageResponse<UserRow> users(String search, String role, Boolean suspended, int page, int size) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)));
            }
            if (role != null && !role.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("role"), Role.valueOf(role.trim().toUpperCase())));
                } catch (IllegalArgumentException ignored) {
                    // unknown role: no filter
                }
            }
            if (Boolean.TRUE.equals(suspended)) {
                predicates.add(cb.equal(root.get("suspended"), true));
            } else if (Boolean.FALSE.equals(suspended)) {
                predicates.add(cb.or(cb.isNull(root.get("suspended")), cb.equal(root.get("suspended"), false)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> users = userRepository.findAll(spec, pageable(page, size, "createdAt"));
        return PageResponse.from(users.map(this::toUserRow));
    }

    @Transactional
    public UserRow setRole(Long id, Role role, String admin) {
        User target = requireUser(id);
        if (target.getUsername().equals(admin)) {
            throw new IllegalArgumentException("You cannot change your own role");
        }
        if (target.getRole() == Role.ROLE_ADMIN && role != Role.ROLE_ADMIN
                && userRepository.countByRole(Role.ROLE_ADMIN) <= 1) {
            throw new IllegalArgumentException("There must always be at least one administrator");
        }
        Role previous = target.getRole();
        target.setRole(role);
        userRepository.save(target);
        auditService.log(admin, "USER_ROLE_CHANGED", "USER", id,
                target.getUsername() + ": " + previous + " -> " + role);
        return toUserRow(target);
    }

    @Transactional
    public UserRow suspend(Long id, String reason, String admin) {
        User target = requireUser(id);
        if (target.getUsername().equals(admin)) {
            throw new IllegalArgumentException("You cannot suspend yourself");
        }
        if (target.getRole() == Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Remove the administrator role first, then suspend the account");
        }
        target.setSuspended(true);
        target.setSuspendedReason(reason == null || reason.isBlank() ? null : reason.trim());
        target.setSuspendedAt(LocalDateTime.now());
        userRepository.save(target);
        auditService.log(admin, "USER_SUSPENDED", "USER", id,
                target.getUsername() + (reason == null || reason.isBlank() ? "" : ": " + reason.trim()));
        return toUserRow(target);
    }

    @Transactional
    public UserRow unsuspend(Long id, String admin) {
        User target = requireUser(id);
        target.setSuspended(false);
        target.setSuspendedReason(null);
        target.setSuspendedAt(null);
        userRepository.save(target);
        auditService.log(admin, "USER_UNSUSPENDED", "USER", id, target.getUsername());
        return toUserRow(target);
    }

    // -------------------------------------------------------------- resources

    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> resources(String search, ResourceStatus status, int page, int size) {
        Specification<Resource> spec = ResourceSpecification.buildFilter(search, null, null, status, null);
        Page<Resource> resources = resourceRepository.findAll(spec, pageable(page, size, "createdAt"));
        return PageResponse.from(resources.map(resourceMapper::toResponse));
    }

    @Transactional
    public void setResourceStatus(Long id, ResourceStatus status, String admin) {
        resourceService.changeStatus(id, status, admin);
        auditService.log(admin, "RESOURCE_STATUS_CHANGED", "RESOURCE", id, "-> " + status);
    }

    @Transactional
    public void deleteResource(Long id, String admin) {
        resourceService.delete(id, admin);
        auditService.log(admin, "RESOURCE_DELETED", "RESOURCE", id, null);
    }

    // --------------------------------------------------------------- comments

    @Transactional(readOnly = true)
    public PageResponse<CommentRow> comments(String search, int page, int size) {
        Pageable pageable = pageable(page, size, "createdAt");
        Page<Comment> comments = search == null || search.isBlank()
                ? commentRepository.findAll(pageable)
                : commentRepository.findByContentContainingIgnoreCase(search.trim(), pageable);
        return PageResponse.from(comments.map(c -> new CommentRow(
                c.getId(),
                clip(c.getContent(), 300),
                c.getUser().getUsername(),
                c.getResource().getId(),
                c.getResource().getTitle(),
                c.getCreatedAt())));
    }

    @Transactional
    public void deleteComment(Long id, String admin) {
        commentService.delete(id, admin);
        auditService.log(admin, "COMMENT_DELETED", "COMMENT", id, null);
    }

    // ---------------------------------------------------------------- reports

    @Transactional(readOnly = true)
    public PageResponse<ReportRow> reports(Report.Status status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<Report> reports = status == null
                ? reportRepository.findAllByOrderByCreatedAtDesc(pageable)
                : reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return PageResponse.from(reports.map(this::toReportRow));
    }

    /**
     * Closes a report, optionally acting on the reported item in the same step
     * (archive / delete the post, delete the comment, suspend the user).
     */
    @Transactional
    public ReportRow resolveReport(Long id, ResolveRequest request, String admin) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Report", id));
        if (request.status() == Report.Status.OPEN) {
            throw new IllegalArgumentException("A report can only be resolved or dismissed");
        }

        String action = request.action() == null ? "NONE" : request.action().trim().toUpperCase();
        if (request.status() == Report.Status.RESOLVED && !"NONE".equals(action)) {
            applyReportAction(report, action, admin);
        }

        User reviewer = userRepository.findByUsername(admin).orElse(null);
        report.setStatus(request.status());
        report.setReviewedBy(reviewer);
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        reportRepository.save(report);

        auditService.log(admin, "REPORT_" + request.status(), "REPORT", id,
                report.getTargetType() + " #" + report.getTargetId()
                        + ("NONE".equals(action) ? "" : " | action " + action));
        return toReportRow(report);
    }

    private void applyReportAction(Report report, String action, String admin) {
        Long target = report.getTargetId();
        try {
            switch (report.getTargetType()) {
                case RESOURCE -> {
                    if ("ARCHIVE".equals(action)) {
                        setResourceStatus(target, ResourceStatus.ARCHIVED, admin);
                    } else if ("DELETE".equals(action)) {
                        deleteResource(target, admin);
                    } else {
                        throw new IllegalArgumentException("Action not valid for a post: use ARCHIVE or DELETE");
                    }
                }
                case COMMENT -> {
                    if ("DELETE".equals(action)) {
                        deleteComment(target, admin);
                    } else {
                        throw new IllegalArgumentException("Action not valid for a comment: use DELETE");
                    }
                }
                case USER -> {
                    if ("SUSPEND".equals(action)) {
                        suspend(target, "Report #" + report.getId(), admin);
                    } else {
                        throw new IllegalArgumentException("Action not valid for a user: use SUSPEND");
                    }
                }
            }
        } catch (ResourceNotFoundException alreadyGone) {
            // The reported item was removed meanwhile - the report can still be closed.
        }
    }

    // ------------------------------------------------------------------ audit

    @Transactional(readOnly = true)
    public PageResponse<AuditRow> audit(int page, int size) {
        Page<com.setec.forum.model.AuditLog> logs =
                auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page, 0), clampSize(size)));
        return PageResponse.from(logs.map(l -> new AuditRow(
                l.getId(), l.getAdminUsername(), l.getAction(), l.getTargetType(),
                l.getTargetId(), l.getDetails(), l.getCreatedAt())));
    }

    // ---------------------------------------------------------------- helpers

    private UserRow toUserRow(User user) {
        return new UserRow(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getProfile() != null ? user.getProfile().getFullName() : null,
                user.getProfile() != null ? user.getProfile().getAvatarUrl() : null,
                user.getRole().name(),
                user.isSuspendedAccount(),
                user.getSuspendedReason(),
                user.getCreatedAt(),
                resourceRepository.countByUserId(user.getId()),
                commentRepository.countByUserId(user.getId()));
    }

    private ReportRow toReportRow(Report r) {
        String label = "(deleted)";
        String owner = null;
        Long resourceId = null;

        switch (r.getTargetType()) {
            case RESOURCE -> {
                Resource resource = resourceRepository.findById(r.getTargetId()).orElse(null);
                if (resource != null) {
                    label = resource.getTitle();
                    owner = resource.getUser().getUsername();
                    resourceId = resource.getId();
                }
            }
            case COMMENT -> {
                Comment comment = commentRepository.findById(r.getTargetId()).orElse(null);
                if (comment != null) {
                    label = clip(comment.getContent(), 120);
                    owner = comment.getUser().getUsername();
                    resourceId = comment.getResource().getId();
                }
            }
            case USER -> {
                User user = userRepository.findById(r.getTargetId()).orElse(null);
                if (user != null) {
                    label = "@" + user.getUsername();
                    owner = user.getUsername();
                }
            }
        }

        return new ReportRow(
                r.getId(), r.getTargetType().name(), r.getTargetId(), label, owner, resourceId,
                r.getReason().name(), r.getDetails(), r.getStatus().name(),
                r.getReporter().getUsername(),
                r.getReviewedBy() != null ? r.getReviewedBy().getUsername() : null,
                r.getReviewNote(), r.getCreatedAt(), r.getReviewedAt());
    }

    private User requireUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private Pageable pageable(int page, int size, String sortBy) {
        return PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by(Sort.Direction.DESC, sortBy));
    }

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    private String clip(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
