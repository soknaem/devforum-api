package com.setec.forum.feature.report;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.model.Report;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST /api/v1/reports  {"targetType":"RESOURCE","targetId":5,"reason":"SPAM","details":"..."} */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    public record ReportRequest(
            @NotNull(message = "targetType is required") Report.Target targetType,
            @NotNull(message = "targetId is required") Long targetId,
            @NotNull(message = "reason is required") Report.Reason reason,
            @Size(max = 500, message = "Details must not exceed 500 characters") String details) {
    }

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(@Valid @RequestBody ReportRequest request,
                                                    Authentication authentication) {
        reportService.create(authentication.getName(), request.targetType(), request.targetId(),
                request.reason(), request.details());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thank you. An administrator will review your report.", null));
    }
}
