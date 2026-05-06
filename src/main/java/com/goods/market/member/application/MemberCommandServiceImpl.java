package com.goods.market.member.application;

import com.goods.market.common.event.DomainEventPublisher;
import com.goods.market.common.event.events.MemberWithdrawnEvent;
import com.goods.market.member.application.dto.MemberSignupCommand;
import com.goods.market.member.application.dto.MemberUpdateCommand;
import com.goods.market.member.domain.Interest;
import com.goods.market.member.domain.Member;
import com.goods.market.member.domain.PhoneNumber;
import com.goods.market.member.infrastructure.Interest.InterestJpaRepository;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
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
public class
MemberCommandServiceImpl implements MemberCommandService {

    private final MemberJpaRepository memberJpaRepository;
    private final InterestJpaRepository interestJpaRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public Long signup(MemberSignupCommand command) {
        PhoneNumber phoneNumber = new PhoneNumber(command.phoneNumber());

        if (memberJpaRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new RuntimeException("이미 존재하는 회원");
        }

        Member member = new Member(command.nickname(), phoneNumber);
        Member saved = memberJpaRepository.save(member);
        return saved.getId();
    }

    @Override
    public void updateNickname(Long memberId, String nickname) {
        Objects.requireNonNull(nickname, "닉네임은 필수입니다");
        Member member = findMemberById(memberId);
        member.updateNickname(nickname);
    }

    @Override
    public void updateMemberInfo(Long memberId, MemberUpdateCommand command) {
        Member member = findMemberById(memberId);
        Optional.ofNullable(command.nickname()).ifPresent(member::updateNickname);
        Optional.ofNullable(command.profileImage()).ifPresent(member::updateProfileImage);
    }

    @Override
    public void updateProfileImage(Long memberId, String profileImage) {
        Objects.requireNonNull(profileImage, "프로필 이미지 경로는 필수입니다");
        Member member = findMemberById(memberId);
        member.updateProfileImage(profileImage);
    }

    @Override
    public void addInterest(Long memberId, Long listingId) {
        Objects.requireNonNull(listingId, "listingId는 필수입니다");

        if (interestJpaRepository.existsByListingIdAndMemberId(listingId, memberId)) {
            log.info("이미 존재하는 관심 매물입니다");
            return;
        }

        Member member = findMemberById(memberId);
        member.addInterest(new Interest(listingId));
    }

    @Override
    public void deleteInterest(Long memberId, Long listingId) {
        Objects.requireNonNull(listingId, "listingId는 필수입니다");

        int result = interestJpaRepository.deleteByListingIdAndMemberId(listingId, memberId);
        if (result == 0) {
            log.info("이미 삭제된 관심 항목입니다. memberId: {}, listingId: {}", memberId, listingId);
        }
    }

    @Override
    public void withdraw(Long memberId) {
        Member member = findMemberById(memberId);
        member.withdraw();
        // 회원 탈퇴 이벤트를 발행한다.
        domainEventPublisher.publish(new MemberWithdrawnEvent(memberId));
    }

    private Member findMemberById(Long memberId) {
        return memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾지 못함"));
    }
}
