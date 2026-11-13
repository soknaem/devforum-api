package com.setec.forum.feature.comment.service;

import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.comment.*;
import com.setec.forum.feature.comment.dto.request.CommentRequest;
import com.setec.forum.feature.comment.dto.response.CommentResponse;

public interface CommentService {
    CommentResponse create(Long resourceId, CommentRequest request, String currentUsername);
    PageResponse<CommentResponse> getTopLevelForResource(Long resourceId, int page, int size, String viewerUsername);
    PageResponse<CommentResponse> getReplies(Long parentId, int page, int size, String viewerUsername);
    CommentResponse update(Long commentId, CommentRequest request, String currentUsername);
    void delete(Long commentId, String currentUsername);
}
