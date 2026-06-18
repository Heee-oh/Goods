package com.goods.market.member.application;

import com.goods.market.common.event.DomainEventPublisher;
import com.goods.market.common.event.events.MemberWithdrawnEvent;
import com.goods.market.member.application.dto.MemberSignupCommand;
import com.goods.market.member.application.dto.MemberUpdateCommand;
import com.goods.market.member.domain.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class MemberCommandService {


    private final InterestRepository interestRepository;
    private final MemberRepository memberRepository;

    private final DomainEventPublisher domainEventPublisher;

    public Long signup(MemberSignupCommand command) {
        PhoneNumber phoneNumber = new PhoneNumber(command.phoneNumber());

        if (memberRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new RuntimeException("이미 존재하는 회원");
        }

        Member member = new Member(command.nickname(), phoneNumber);
        Member saved = memberRepository.save(member);
        return saved.getId();
    }
    public void updateNickname(Long memberId, String nickname) {
        Objects.requireNonNull(nickname, "닉네임은 필수입니다");
        Member member = findMemberById(memberId);
        member.updateNickname(nickname);
    }
    public void updateMemberInfo(Long memberId, MemberUpdateCommand command) {
        Member member = findMemberById(memberId);
        Optional.ofNullable(command.nickname()).ifPresent(member::updateNickname);
        Optional.ofNullable(command.profileImage()).ifPresent(member::updateProfileImage);
    }
    public void updateProfileImage(Long memberId, String profileImage) {
        Objects.requireNonNull(profileImage, "프로필 이미지 경로는 필수입니다");
        Member member = findMemberById(memberId);
        member.updateProfileImage(profileImage);
    }
    public void addInterest(Long memberId, Long listingId) {
        Objects.requireNonNull(listingId, "listingId는 필수입니다");

        if (interestRepository.existsByListingIdAndMemberId(listingId, memberId)) {
            log.info("이미 존재하는 관심 매물입니다");
            return;
        }

        interestRepository.save(new Interest(listingId, memberId));
    }

    public void deleteInterest(Long memberId, Long listingId) {
        Objects.requireNonNull(listingId, "listingId는 필수입니다");

        int result = interestRepository.deleteByListingIdAndMemberId(listingId, memberId);
        if (result == 0) {
            log.info("이미 삭제된 관심 항목입니다. memberId: {}, listingId: {}", memberId, listingId);
        }
    }
    public void withdraw(Long memberId) {
        Member member = findMemberById(memberId);
        member.withdraw();
        // 회원 탈퇴 이벤트를 발행한다.
        domainEventPublisher.publish(new MemberWithdrawnEvent(memberId));
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾지 못함"));
    }
}
