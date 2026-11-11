package com.setec.forum.feature.resource;

import com.setec.forum.model.Resource;
import com.setec.forum.model.ResourceStatus;
import com.setec.forum.model.ResourceType;
import com.setec.forum.model.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class ResourceSpecification {

    private ResourceSpecification() {
    }

    public static Specification<Resource> search(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    public static Specification<Resource> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("category").get("id"), categoryId);
        };
    }

    public static Specification<Resource> hasType(ResourceType type) {
        return (root, query, cb) -> {
            if (type == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("type"), type);
        };
    }

    public static Specification<Resource> hasStatus(ResourceStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Resource> hasTag(String tagName) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(tagName)) {
                return cb.conjunction();
            }
            query.distinct(true);
            Join<Resource, Tag> tagJoin = root.join("tags", JoinType.INNER);
            return cb.equal(cb.lower(tagJoin.get("name")), tagName.toLowerCase());
        };
    }

    /** Hides resources written by any of these users (blocked either way). */
    public static Specification<Resource> notAuthoredBy(java.util.Collection<Long> userIds) {
        return (root, query, cb) -> {
            if (userIds == null || userIds.isEmpty()) {
                return cb.conjunction();
            }
            return cb.not(root.get("user").get("id").in(userIds));
        };
    }

    /**
     * Combines all optional filters into a single Specification, ignoring any null/blank criteria.
     */
    public static Specification<Resource> buildFilter(String search, Long categoryId, ResourceType type,
                                                        ResourceStatus status, String tag) {
        List<Specification<Resource>> specs = new ArrayList<>();
        specs.add(search(search));
        specs.add(hasCategoryId(categoryId));
        specs.add(hasType(type));
        specs.add(hasStatus(status));
        specs.add(hasTag(tag));

        Specification<Resource> combined = Specification.where(null);
        for (Specification<Resource> spec : specs) {
            combined = combined.and(spec);
        }
        return combined;
    }
}
