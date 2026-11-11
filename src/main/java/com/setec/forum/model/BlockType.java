package com.setec.forum.model;

/** What one block of a resource's body contains. */
public enum BlockType {
    /** A paragraph of text. */
    TEXT,
    /** An uploaded file: shown inline when it is an image / video, as a download card otherwise. */
    MEDIA
}
