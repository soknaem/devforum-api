package com.setec.forum.feature.media.dto.response;

import com.setec.forum.model.MediaType;
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
public class MediaResponse {
    private Long id;
    private String fileName;
    private String url;
    private String contentType;
    private long sizeBytes;
    private MediaType mediaType;
    private Long resourceId;
}
