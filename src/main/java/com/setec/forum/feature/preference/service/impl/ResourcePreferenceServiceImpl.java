package com.setec.forum.feature.preference.service.impl;

import com.setec.forum.exception.ResourceNotFoundException;
import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.preference.*;
import com.setec.forum.feature.preference.dto.response.PreferenceResponse;
import com.setec.forum.feature.preference.service.ResourcePreferenceService;
import com.setec.forum.feature.resource.ResourceRepository;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.model.PreferenceType;
import com.setec.forum.model.Resource;
import com.setec.forum.model.ResourcePreference;
import com.setec.forum.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourcePreferenceServiceImpl implements ResourcePreferenceService {

    private final ResourcePreferenceRepository preferenceRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    /** Upsert: choosing the other option swaps the existing row instead of creating a second one. */
    @Transactional
    @Override
    public PreferenceResponse set(Long resourceId, PreferenceType type, String currentUsername) {
        User user = requireUser(currentUsername);
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Resource", resourceId));

        if (resource.getUser() != null && resource.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You cannot set a preference on your own resource");
        }

        ResourcePreference preference = preferenceRepository
                .findByUserIdAndResourceId(user.getId(), resourceId)
                .orElseGet(() -> ResourcePreference.builder().user(user).resource(resource).build());
        preference.setType(type);
        preferenceRepository.save(preference);

        return PreferenceResponse.builder().resourceId(resourceId).preference(type).build();
    }

    /** Undo ("Undo" after hiding, or un-marking "Interested"). Idempotent. */
    @Transactional
    @Override
    public PreferenceResponse clear(Long resourceId, String currentUsername) {
        User user = requireUser(currentUsername);
        preferenceRepository.deleteByUserIdAndResourceId(user.getId(), resourceId);
        return PreferenceResponse.builder().resourceId(resourceId).preference(null).build();
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }
}
