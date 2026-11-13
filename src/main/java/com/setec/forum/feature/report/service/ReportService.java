package com.setec.forum.feature.report.service;

import com.setec.forum.feature.report.*;
import com.setec.forum.model.Report;

public interface ReportService {
    void create(String reporterUsername, Report.Target target, Long targetId,
                       Report.Reason reason, String details);
}
