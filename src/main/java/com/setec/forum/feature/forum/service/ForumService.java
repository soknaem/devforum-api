package com.setec.forum.feature.forum.service;

import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.forum.dto.ForumCategoryResponse;
import com.setec.forum.feature.forum.dto.ForumReplyRequest;
import com.setec.forum.feature.forum.dto.ForumReplyResponse;
import com.setec.forum.feature.forum.dto.ForumTopicRequest;
import com.setec.forum.feature.forum.dto.ForumTopicResponse;
import com.setec.forum.feature.share.dto.request.ShareRequest;

import java.util.List;

public interface ForumService {
    List<ForumCategoryResponse> getCategories();
    PageResponse<ForumTopicResponse> getTopics(String categoryKey, String sortBy, int page, int size, String username);
    ForumTopicResponse getTopic(Long topicId, String username);
    PageResponse<ForumReplyResponse> getReplies(Long topicId, int page, int size);
    ForumTopicResponse createTopic(ForumTopicRequest request, String username);
    ForumReplyResponse createReply(Long topicId, ForumReplyRequest request, String username);
    ForumTopicResponse shareTopic(Long topicId, ShareRequest request, String username);
    void deleteTopic(Long topicId, String username);
}