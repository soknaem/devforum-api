package com.setec.forum.mapper;

import com.setec.forum.feature.auth.dto.request.RegisterRequest;
import com.setec.forum.feature.auth.dto.response.AuthResponse;
import com.setec.forum.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "resources", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(RegisterRequest request);

    @Mapping(target = "token", ignore = true)
    @Mapping(target = "tokenType", ignore = true)
    @Mapping(target = "userId", source = "id")
    AuthResponse toAuthResponse(User user);

    default AuthResponse toAuthResponse(User user, String token) {
        AuthResponse response = toAuthResponse(user);
        response.setToken(token);
        response.setTokenType("Bearer");
        return response;
    }
}
