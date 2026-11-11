package com.setec.forum.feature.interest.dto;

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
public class InterestsResponse {
    /** Tags the user picked. */
    private List<String> tags;
    /** Tags learned from what the user reads, saves and reacts to (not picked by them). */
    private List<String> learned;
    /** Popular tags that can be added with one click. */
    private List<String> suggestions;
}
