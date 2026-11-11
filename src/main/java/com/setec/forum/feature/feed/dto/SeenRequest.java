package com.setec.forum.feature.feed.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeenRequest {
    /** Resources that were visible on screen (max 50 per call). */
    private List<Long> resourceIds;
}
