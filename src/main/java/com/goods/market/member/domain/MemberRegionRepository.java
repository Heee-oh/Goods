package com.goods.market.member.domain;

import java.util.Optional;

public interface MemberRegionRepository {
    MemberRegion save(MemberRegion memberRegion);
    Optional<MemberRegion> findById(Long id);
    Optional<MemberRegion> findByMemberIdAndRegionId(Long memberId, Integer regionId);
    Optional<MemberRegion> findPrimaryByMemberIdAndRegionId(Long memberId, Integer regionId);
    Optional<MemberRegion> findVerifiedPrimaryByMemberIdAndRegionId(Long memberId, Integer regionId);
    boolean existsPrimaryByMemberIdAndRegionId(Long memberId, Integer regionId);
    long countPrimaryByMemberId(Long memberId);

}
