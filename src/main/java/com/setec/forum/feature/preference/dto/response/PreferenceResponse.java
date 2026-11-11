package com.setec.forum.feature.preference.dto.response;

import com.setec.forum.model.PreferenceType;
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
public class PreferenceResponse {
    private Long resourceId;

    /** The viewer's current choice, or null after it was cleared. */
    private PreferenceType preference;
}
