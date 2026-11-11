package com.setec.forum.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.setec.forum.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
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
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {

    @Id
    private Long id;

    @Column(length = 100)
    private String fullName;

    @Column(length = 500)
    private String bio;

    @Column(length = 255)
    private String avatarUrl;

    @Column(length = 255)
    private String coverUrl;

    @Column(length = 255)
    private String location;

    @Column(length = 255)
    private String website;

    // 1:1 mapping - shares the primary key of the associated User
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    @JsonIgnore
    private User user;
}
