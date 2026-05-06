package com.goods.market.common.auth.application;

import com.goods.market.common.auth.application.dto.AuthLoginCommand;
import com.goods.market.common.auth.application.dto.AuthSignupCommand;
import com.goods.market.common.auth.application.dto.AuthTokenResponse;
import com.goods.market.common.auth.exception.AuthBadRequestException;
import com.goods.market.common.auth.exception.AuthConflictException;
import com.goods.market.common.auth.exception.AuthUnauthorizedException;
import com.goods.market.common.auth.jwt.JwtTokenProvider;
import com.goods.market.member.domain.Member;
import com.goods.market.member.domain.MemberRegion;
import com.goods.market.member.domain.PhoneNumber;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberJpaRepository memberJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public AuthTokenResponse signup(AuthSignupCommand command) {
        PhoneNumber phoneNumber = parsePhoneNumber(command.phoneNumber());
        if (memberJpaRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new AuthConflictException("Phone number is already registered");
        }

        String nickname = resolveNickname(command.nickname(), command.phoneNumber());
        Member member = new Member(nickname, phoneNumber);
        if (command.regionId() != null) {
            member.addRegion(new MemberRegion(command.regionId(), true));
        }
        Member savedMember = memberJpaRepository.save(member);
        String accessToken = jwtTokenProvider.createAccessToken(savedMember.getId());

        return new AuthTokenResponse(savedMember.getId(), accessToken, jwtTokenProvider.getAccessTokenValiditySeconds());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthTokenResponse login(AuthLoginCommand command) {
        PhoneNumber phoneNumber = parsePhoneNumber(command.phoneNumber());

        Member member = memberJpaRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new AuthUnauthorizedException("Member not found for this phone number"));
        String accessToken = jwtTokenProvider.createAccessToken(member.getId());
        return new AuthTokenResponse(member.getId(), accessToken, jwtTokenProvider.getAccessTokenValiditySeconds());
    }

    private PhoneNumber parsePhoneNumber(String rawPhoneNumber) {
        try {
            return new PhoneNumber(rawPhoneNumber);
        } catch (IllegalArgumentException e) {
            throw new AuthBadRequestException("Invalid phone number format");
        }
    }

    private String resolveNickname(String nickname, String phoneNumber) {
        if (nickname != null && !nickname.isBlank() && nickname.length() <= 50) {
            return nickname;
        }

        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "user";
        }

        return "user_" + UUID.randomUUID().toString().substring(0, 7);
    }
}

