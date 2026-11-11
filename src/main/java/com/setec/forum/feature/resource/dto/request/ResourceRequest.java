package com.setec.forum.feature.resource.dto.request;

import com.setec.forum.model.ResourceStatus;
import com.setec.forum.model.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Size(max = 500, message = "URL must not exceed 500 characters")
    private String url;

    @NotNull(message = "Resource type is required")
    private ResourceType type;

    private ResourceStatus status;

    private Long categoryId;

    private List<String> tags;

    /**
     * Optional: allows an admin/service context to explicitly set the author.
     * When absent, the authenticated principal is used as the author.
     */
    private Long userId;
}
