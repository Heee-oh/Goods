package com.goods.market.member.infrastructure.member;

import com.goods.market.member.domain.Member;
import com.goods.market.member.domain.MemberRepository;
import com.goods.market.member.domain.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMemberRepositoryAdapter implements MemberRepository {

    private final MemberJpaRepository memberJpaRepository;

    @Override
    public Member save(Member member) {
        return memberJpaRepository.save(member);
    }

    @Override
    public Optional<Member> findById(Long memberId) {
        return memberJpaRepository.findById(memberId);
    }

    @Override
    public Optional<Member> findByPhoneNumber(PhoneNumber phoneNumber) {
        return memberJpaRepository.findByPhoneNumber(phoneNumber);
    }

}
