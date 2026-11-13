package com.setec.forum.feature.forum.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class ForumTopicRequest {

    @NotBlank
    @Size(min = 8, max = 180)
    private String title;

    @NotBlank
    private String categoryKey;

    @NotEmpty
    @Size(max = 40)
    private List<@Valid ForumContentBlock> blocks;
}
