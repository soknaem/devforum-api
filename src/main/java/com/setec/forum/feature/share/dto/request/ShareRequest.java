package com.setec.forum.feature.share.dto.request;

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
public class ShareRequest {

    /** Optional caption/comment shown above the shared post, Facebook-style. */
    @Size(max = 500)
    private String comment;
}
