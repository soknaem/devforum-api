package com.setec.forum.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Keyset ("infinite scroll") pagination response: instead of a page number, the client sends back
 * `nextCursor` on its next request to fetch the following batch. Preferred over offset pagination
 * for a feed that mutates while a user is scrolling.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageResponse<T> {
    private List<T> content;
    private Long nextCursor;
    private boolean hasMore;
}
