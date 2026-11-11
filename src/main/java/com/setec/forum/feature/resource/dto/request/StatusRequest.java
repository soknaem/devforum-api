package com.setec.forum.feature.resource.dto.request;

import com.setec.forum.model.ResourceStatus;
import jakarta.validation.constraints.NotNull;
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
public class StatusRequest {

    @NotNull(message = "status is required (PUBLISHED, DRAFT or ARCHIVED)")
    private ResourceStatus status;
}
