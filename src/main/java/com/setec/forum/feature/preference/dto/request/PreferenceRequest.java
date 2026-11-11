package com.setec.forum.feature.preference.dto.request;

import com.setec.forum.model.PreferenceType;
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
public class PreferenceRequest {

    @NotNull(message = "type is required (INTERESTED or NOT_INTERESTED)")
    private PreferenceType type;
}
