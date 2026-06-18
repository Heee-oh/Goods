package com.goods.market.common.auth.application;

import com.goods.market.common.auth.application.dto.AuthLoginCommand;
import com.goods.market.common.auth.application.dto.AuthSignupCommand;
import com.goods.market.common.auth.application.dto.AuthTokenDto;
import com.goods.market.common.auth.exception.AuthBadRequestException;
import com.goods.market.common.auth.exception.AuthConflictException;
import com.goods.market.common.auth.exception.AuthUnauthorizedException;
import com.goods.market.common.auth.jwt.JwtTokenProvider;
import com.goods.market.member.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final MemberRegionRepository memberRegionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    public AuthTokenDto signup(AuthSignupCommand command) {
        PhoneNumber phoneNumber = parsePhoneNumber(command.phoneNumber());
        if (memberRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new AuthConflictException("Phone number is already registered");
        }

        String nickname = resolveNickname(command.nickname(), command.phoneNumber());
        Member member = new Member(nickname, phoneNumber);
        Member savedMember = memberRepository.save(member);

        if (command.regionId() != null) {
            MemberRegion memberRegion = new MemberRegion(command.regionId(), true);
            memberRegion.updateMember(savedMember.getId());
            memberRegionRepository.save(memberRegion);
        }

        String accessToken = jwtTokenProvider.createAccessToken(savedMember.getId());

        return new AuthTokenDto(savedMember.getId(), accessToken, jwtTokenProvider.getAccessTokenValiditySeconds());
    }
    @Transactional(readOnly = true)
    public AuthTokenDto login(AuthLoginCommand command) {
        PhoneNumber phoneNumber = parsePhoneNumber(command.phoneNumber());

        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new AuthUnauthorizedException("Member not found for this phone number"));
        String accessToken = jwtTokenProvider.createAccessToken(member.getId());
        return new AuthTokenDto(member.getId(), accessToken, jwtTokenProvider.getAccessTokenValiditySeconds());
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
