package com.setec.forum.feature.share.service;

import com.setec.forum.feature.resource.dto.response.ResourceResponse;
import com.setec.forum.feature.share.*;
import com.setec.forum.feature.share.dto.request.ShareRequest;

public interface ShareService {
    ResourceResponse share(Long resourceId, ShareRequest request, String currentUsername);
}
