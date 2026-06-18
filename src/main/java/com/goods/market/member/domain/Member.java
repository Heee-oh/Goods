package com.goods.market.member.domain;

import com.goods.market.common.domain.BaseTimeEntity;
import com.github.f4b6a3.tsid.TsidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Member extends BaseTimeEntity {

    @Id
    @Column(name = "member_id")
    private Long id;

    @Column(length = 50, nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private MemberStatus status;

    @Column(name = "smile_score", nullable = false, columnDefinition = "integer not null default 100")
    private int smileScore;

    @Embedded
    private PhoneNumber phoneNumber;

    @Column(name = "profile_image", length = 500)
    private String profileImageUrl;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @PrePersist
    void init() {
        if (id == null) {
            id = TsidCreator.getTsid().toLong();
        }
    }

    public Member(String nickname, PhoneNumber phoneNumber) {
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.status = MemberStatus.ACTIVE;
        this.smileScore = 100;
    }

    public void withdraw() {
        if (status != MemberStatus.WITHDRAWN) {
            status = MemberStatus.WITHDRAWN;
            withdrawnAt = Instant.now();
            return;
        }
        throw new IllegalStateException("Already withdrawn member");
    }

    public void updateSmileScore(int select) {
        ensureActive();

        int nSmileScore = getSmileScore(select);
        if (nSmileScore < 0 || nSmileScore > 999) {
            nSmileScore = Math.max(0, Math.min(nSmileScore, 999));
        }

        smileScore = nSmileScore;
    }

    public void updateProfileImage(String fileName) {
        ensureActive();

        if (fileName == null) {
            throw new IllegalArgumentException("Image is required");
        }
        profileImageUrl = fileName;
    }

    public void updateNickname(String nickname) {
        ensureActive();

        if (nickname == null || nickname.isBlank() || nickname.length() > 50) {
            throw new IllegalArgumentException("Nickname must be 1 to 50 characters");
        }

        this.nickname = nickname;
    }

    public void suspend() {
        ensureActive();
        status = MemberStatus.SUSPENDED;
    }

    public void active() {
        if (status != MemberStatus.SUSPENDED) {
            throw new IllegalStateException("Status is not SUSPENDED");
        }

        status = MemberStatus.ACTIVE;
    }

    private void ensureActive() {
        if (status != MemberStatus.ACTIVE) {
            throw new IllegalStateException("Status is not ACTIVE");
        }
    }

    private int getSmileScore(int select) {
        return switch (select) {
            case 1 -> smileScore - 2;
            case 2 -> smileScore - 1;
            case 4 -> smileScore + 1;
            case 5 -> smileScore + 2;
            default -> smileScore;
        };
    }
}
