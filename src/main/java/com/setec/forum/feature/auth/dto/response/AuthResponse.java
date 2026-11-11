package com.setec.forum.feature.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * Internal JWT value used by AuthController to create the HttpOnly cookie.
     * It is never serialized into the JSON response.
     */
    @JsonIgnore
    private String token;

    @JsonIgnore
    @Builder.Default
    private String tokenType = "Bearer";

    private Long userId;
    private String username;
    private String email;
}
