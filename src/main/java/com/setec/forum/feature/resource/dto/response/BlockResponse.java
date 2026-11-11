package com.setec.forum.feature.resource.dto.response;

import com.setec.forum.feature.media.dto.response.MediaResponse;
import com.setec.forum.model.BlockType;
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
public class BlockResponse {
    private Long id;
    private BlockType type;
    private String text;
    private MediaResponse media;
}
