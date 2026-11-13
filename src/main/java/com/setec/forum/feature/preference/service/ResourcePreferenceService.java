package com.setec.forum.feature.preference.service;

import com.setec.forum.feature.preference.*;
import com.setec.forum.feature.preference.dto.response.PreferenceResponse;
import com.setec.forum.model.PreferenceType;

public interface ResourcePreferenceService {
    PreferenceResponse set(Long resourceId, PreferenceType type, String currentUsername);
    PreferenceResponse clear(Long resourceId, String currentUsername);
}
