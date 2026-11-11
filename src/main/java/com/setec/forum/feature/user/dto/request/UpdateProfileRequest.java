package com.setec.forum.feature.user.dto.request;

import jakarta.validation.constraints.Size;
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
public class UpdateProfileRequest {

    @Size(max = 100)
    private String fullName;

    @Size(max = 500)
    private String bio;

    @Size(max = 255)
    private String location;

    @Size(max = 255)
    private String website;
}
