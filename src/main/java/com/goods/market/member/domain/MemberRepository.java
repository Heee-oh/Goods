package com.goods.market.member.domain;

import com.goods.market.listing.domain.Listing;
import org.springframework.stereotype.Repository;

import java.util.Optional;


public interface MemberRepository {
    Member save(Member member);
    Optional<Member> findById(Long memberId);
    Optional<Member> findByPhoneNumber(PhoneNumber phoneNumber);

}
