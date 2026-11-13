package com.setec.forum.feature.admin.service.impl;

import com.setec.forum.feature.admin.*;
import com.setec.forum.feature.admin.service.AuditService;
import com.setec.forum.model.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every administrator action is written here, in the same transaction as the action itself. */
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    @Override
    public void log(String admin, String action, String targetType, Long targetId, String details) {
        String clipped = details != null && details.length() > 500 ? details.substring(0, 500) : details;
        auditLogRepository.save(AuditLog.builder()
                .adminUsername(admin)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(clipped)
                .build());
    }
}
