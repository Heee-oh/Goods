package com.goods.market.member.domain;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.util.Reflection;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

@Slf4j
class MemberTest {


    @Test
    @DisplayName("매너 온도 0이하 내려감 방지 테스트")
    void updateSmileScoreMinTest() {
        Member member = new Member(null, null);
        member.updateNickname("ABC");

        // m
        for (int i = 0; i < 365; i++) {
            member.updateSmileScore(1);
        }
        Assertions.assertThat(member.getSmileScore()).isEqualTo(0);
    }


    @Test
    @DisplayName("매너 온도 상한 99.9 방지 테스트")
    void updateSmileScoreMaxTest() {
        Member member = new Member(null, null);
        member.updateNickname("ABC");

        for (int i = 0; i < 1000; i++) {
            member.updateSmileScore(5);
        }
        Assertions.assertThat(member.getSmileScore()).isEqualTo(999);

    }

    @Test
    @DisplayName("프로필 이미지 업데이트 테스트")
    void updateProfileImage() {
        Member member = new Member(null, null);

        Assertions.assertThatThrownBy(() -> member.updateProfileImage(null))
                .isInstanceOf(IllegalArgumentException.class);

        member.updateProfileImage("abc.png");

        Assertions.assertThat(member.getProfileImageUrl()).isEqualTo("abc.png");
    }

    @Test
    @DisplayName("정지 상태 테스트")
    void suspendTest() {
        Member member = new Member(null, null);
        Assertions.assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        member.suspend();

        Assertions.assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);

        log.info("정지하고 active 테스트");
        member.active();
        Assertions.assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);

        log.info("탈퇴후 정지 시도 -> 예외던짐");
        member.withdraw();
        Assertions.assertThatThrownBy(() -> member.suspend())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("탈퇴 테스트")
    void withdrawnTest() {
        Member member = new Member(null, null);
        Assertions.assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);

        log.info("탈퇴후 상태와 날짜 체크 ");
        member.withdraw();
        Assertions.assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        Assertions.assertThat(member.getWithdrawnAt()).isNotNull();

    }



}
