package com.goods.market.common.auth.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            "daangn-market-default-secret-key-change-this-please-2026",
            3600L
    );

    JwtTokenProviderTest() {
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("JWT 생성 후 memberId를 추출할 수 있다")
    void createAndParseToken() {
        String token = jwtTokenProvider.createAccessToken(123L);

        assertThat(jwtTokenProvider.validate(token)).isTrue();
        assertThat(jwtTokenProvider.getMemberId(token)).isEqualTo(123L);
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 검증에 실패한다")
    void invalidToken() {
        assertThat(jwtTokenProvider.validate("invalid-token")).isFalse();
    }
}

