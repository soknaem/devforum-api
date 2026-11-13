package com.setec.forum.feature.bookmark.service;

import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.bookmark.*;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;

public interface BookmarkService {
    void save(Long resourceId, String currentUsername);
    void remove(Long resourceId, String currentUsername);
    PageResponse<ResourceResponse> getMyBookmarks(String currentUsername, int page, int size);
}
