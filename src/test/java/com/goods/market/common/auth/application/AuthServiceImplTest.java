package com.goods.market.common.auth.application;

import com.goods.market.common.auth.application.dto.AuthLoginCommand;
import com.goods.market.common.auth.application.dto.AuthSignupCommand;
import com.goods.market.common.auth.application.dto.AuthTokenResponse;
import com.goods.market.common.auth.exception.AuthConflictException;
import com.goods.market.common.auth.exception.AuthUnauthorizedException;
import com.goods.market.common.auth.jwt.JwtTokenProvider;
import com.goods.market.member.domain.Member;
import com.goods.market.member.domain.PhoneNumber;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private MemberJpaRepository memberJpaRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("signup: 새 전화번호면 가입 후 토큰을 반환한다")
    void signupSuccess() {
        when(memberJpaRepository.findByPhoneNumber(any(PhoneNumber.class))).thenReturn(Optional.empty());
        when(memberJpaRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            ReflectionTestUtils.setField(member, "id", 101L);
            return member;
        });
        when(jwtTokenProvider.createAccessToken(101L)).thenReturn("token-101");
        when(jwtTokenProvider.getAccessTokenValiditySeconds()).thenReturn(3600L);

        AuthTokenResponse response = authService.signup(new AuthSignupCommand("01012345678", "alice", 1));

        assertThat(response.memberId()).isEqualTo(101L);
        assertThat(response.accessToken()).isEqualTo("token-101");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        verify(jwtTokenProvider).createAccessToken(101L);
    }

    @Test
    @DisplayName("signup: 이미 존재하는 전화번호면 충돌 예외를 던진다")
    void signupConflict() {
        when(memberJpaRepository.findByPhoneNumber(any(PhoneNumber.class)))
                .thenReturn(Optional.of(new Member("dup", new PhoneNumber("01012345678"))));

        assertThatThrownBy(() -> authService.signup(new AuthSignupCommand("01012345678", "alice", 1)))
                .isInstanceOf(AuthConflictException.class);
    }

    @Test
    @DisplayName("login: 가입된 전화번호면 토큰을 반환한다")
    void loginSuccess() {
        Member member = new Member("user", new PhoneNumber("01012345678"));
        ReflectionTestUtils.setField(member, "id", 10L);

        when(memberJpaRepository.findByPhoneNumber(any(PhoneNumber.class))).thenReturn(Optional.of(member));
        when(jwtTokenProvider.createAccessToken(10L)).thenReturn("token-10");
        when(jwtTokenProvider.getAccessTokenValiditySeconds()).thenReturn(3600L);

        AuthTokenResponse response = authService.login(new AuthLoginCommand("01012345678"));

        assertThat(response.memberId()).isEqualTo(10L);
        assertThat(response.accessToken()).isEqualTo("token-10");
    }

    @Test
    @DisplayName("login: 없는 전화번호면 인증 예외를 던진다")
    void loginFailWhenNotFound() {
        when(memberJpaRepository.findByPhoneNumber(any(PhoneNumber.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new AuthLoginCommand("01000000000")))
                .isInstanceOf(AuthUnauthorizedException.class);
    }
}

