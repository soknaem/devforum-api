package com.setec.forum.model;

/**
 * How a user feels about somebody else's resource (the "..." menu on a post).
 */
public enum PreferenceType {
    /** "Interested" - the user wants to see more like this. */
    INTERESTED,
    /** "Not interested" - the resource is hidden from this user's feed. */
    NOT_INTERESTED
}
