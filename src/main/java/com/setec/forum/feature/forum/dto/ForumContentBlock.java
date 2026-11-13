package com.setec.forum.feature.forum.dto;

import com.setec.forum.model.ForumBlockType;
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
public class ForumContentBlock {

    private ForumBlockType type;

    @Size(max = 30000, message = "A content block is too long")
    private String text;

    @Size(max = 40)
    private String language;

    @Size(max = 2048)
    private String url;

    @Size(max = 200)
    private String title;

    @Size(max = 200)
    private String alt;

    private Long mediaId;

    @Size(max = 255)
    private String fileName;

    @Size(max = 100)
    private String contentType;
}
