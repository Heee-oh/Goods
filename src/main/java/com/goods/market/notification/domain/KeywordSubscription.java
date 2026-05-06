package com.goods.market.notification.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(name = "keyword_subscription")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KeywordSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(length = 100, nullable = false)
    private String keyword;

    @Column(name = "region_id")
    private Integer regionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
