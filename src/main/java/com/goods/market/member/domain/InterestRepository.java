package com.goods.market.member.domain;

import java.util.Optional;

public interface InterestRepository {

    Interest save(Interest interest);

    Optional<Interest> findById(Long id);

    boolean existsByListingIdAndMemberId(Long listingId, Long memberId);

    int deleteByListingIdAndMemberId(Long listingId, Long memberId);
}
