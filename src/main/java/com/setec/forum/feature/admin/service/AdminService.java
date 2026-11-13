package com.setec.forum.feature.admin.service;

import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.admin.*;
import com.setec.forum.feature.admin.AdminDtos.AuditRow;
import com.setec.forum.feature.admin.AdminDtos.CommentRow;
import com.setec.forum.feature.admin.AdminDtos.ForumReplyRow;
import com.setec.forum.feature.admin.AdminDtos.ForumTopicRow;
import com.setec.forum.feature.admin.AdminDtos.ReportRow;
import com.setec.forum.feature.admin.AdminDtos.ResolveRequest;
import com.setec.forum.feature.admin.AdminDtos.Stats;
import com.setec.forum.feature.admin.AdminDtos.UserRow;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.model.Report;
import com.setec.forum.model.ResourceStatus;
import com.setec.forum.model.Role;

public interface AdminService {
    Stats stats();
    PageResponse<UserRow> users(String search, String role, Boolean suspended, int page, int size);
    UserRow setRole(Long id, Role role, String admin);
    UserRow suspend(Long id, String reason, String admin);
    UserRow unsuspend(Long id, String admin);
    PageResponse<ResourceResponse> resources(String search, ResourceStatus status, int page, int size);
    void setResourceStatus(Long id, ResourceStatus status, String admin);
    void deleteResource(Long id, String admin);
    PageResponse<CommentRow> comments(String search, int page, int size);
    void deleteComment(Long id, String admin);
    PageResponse<ForumTopicRow> forumTopics(String search, String categoryKey, int page, int size);
    void deleteForumTopic(Long id, String admin);
    PageResponse<ForumReplyRow> forumReplies(String search, int page, int size);
    void deleteForumReply(Long id, String admin);
    PageResponse<ReportRow> reports(Report.Status status, int page, int size);
    ReportRow resolveReport(Long id, ResolveRequest request, String admin);
    PageResponse<AuditRow> audit(int page, int size);
}