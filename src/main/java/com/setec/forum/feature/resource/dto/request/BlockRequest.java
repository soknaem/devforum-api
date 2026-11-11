package com.setec.forum.feature.resource.dto.request;

import com.setec.forum.model.BlockType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One block of a resource body, as sent by the editor.
 *
 *  TEXT  -> {@code text}
 *  MEDIA -> either {@code mediaId} (a file already attached to this resource) or
 *           {@code fileIndex} (position of a NEW file in the multipart "files" list)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockRequest {
    private BlockType type;
    private String text;
    private Long mediaId;
    private Integer fileIndex;
}
