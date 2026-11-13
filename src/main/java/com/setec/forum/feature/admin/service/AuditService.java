package com.setec.forum.feature.admin.service;

import com.setec.forum.feature.admin.*;

public interface AuditService {
    void log(String admin, String action, String targetType, Long targetId, String details);
}
