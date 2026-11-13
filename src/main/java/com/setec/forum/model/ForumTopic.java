package com.setec.forum.model;

import com.setec.forum.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
@Entity
@Table(name = "forum_topics", indexes = {
        @Index(name = "idx_forum_topics_category", columnList = "category_key"),
        @Index(name = "idx_forum_topics_activity", columnList = "updated_at")
})
public class ForumTopic extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "category_key", nullable = false, length = 40)
    private String categoryKey;

    @Column(length = 260)
    private String summary;

    @Lob
    @Column(name = "body_json", nullable = false)
    private String bodyJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "reply_count", nullable = false)
    @Builder.Default
    private long replyCount = 0;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private long viewCount = 0;

    /**
     * Set when this topic is a share (repost) of another topic. The sharer's caption is this topic's
     * own content; the original is read live through this link. Shares never point at another share.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_from_id")
    private ForumTopic sharedFrom;
}