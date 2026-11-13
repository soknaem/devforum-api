package com.setec.forum.feature.user.service;

import com.setec.forum.base.PageResponse;
import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.feature.user.*;
import com.setec.forum.model.Resource;

public interface ResourceViewService {
    void recordView(Resource resource, String viewerUsername);
    PageResponse<ResourceResponse> getHistory(String username, int page, int size);
}
