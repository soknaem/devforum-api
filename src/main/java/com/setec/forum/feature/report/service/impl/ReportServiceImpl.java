package com.setec.forum.feature.report.service.impl;

import com.setec.forum.exception.ResourceNotFoundException;
import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.comment.CommentRepository;
import com.setec.forum.feature.report.*;
import com.setec.forum.feature.report.service.ReportService;
import com.setec.forum.feature.resource.ResourceRepository;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.model.Comment;
import com.setec.forum.model.Report;
import com.setec.forum.model.Resource;
import com.setec.forum.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lets any signed-in user report a post, a comment or a user. Administrators review them. */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final CommentRepository commentRepository;

    @Transactional
    @Override
    public void create(String reporterUsername, Report.Target target, Long targetId,
                       Report.Reason reason, String details) {
        User reporter = userRepository.findByUsername(reporterUsername)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));

        // The reported thing must exist, and people cannot report themselves.
        Long ownerId;
        switch (target) {
            case RESOURCE -> {
                Resource resource = resourceRepository.findById(targetId)
                        .orElseThrow(() -> ResourceNotFoundException.of("Resource", targetId));
                ownerId = resource.getUser().getId();
            }
            case COMMENT -> {
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> ResourceNotFoundException.of("Comment", targetId));
                ownerId = comment.getUser().getId();
            }
            default -> {
                User user = userRepository.findById(targetId)
                        .orElseThrow(() -> ResourceNotFoundException.of("User", targetId));
                ownerId = user.getId();
            }
        }
        if (ownerId.equals(reporter.getId())) {
            throw new IllegalArgumentException("You cannot report your own content");
        }

        // Reporting the same thing twice while the first report is still open changes nothing.
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporter.getId(), target, targetId, Report.Status.OPEN)) {
            return;
        }

        String clean = details == null || details.isBlank() ? null : details.trim();
        reportRepository.save(Report.builder()
                .reporter(reporter)
                .targetType(target)
                .targetId(targetId)
                .reason(reason)
                .details(clean)
                .build());
    }
}
