package com.goods.market.member.infrastructure.memberRegion;

import com.goods.market.member.domain.MemberRegion;
import com.goods.market.member.domain.MemberRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMemberRegionRepositoryAdapter implements MemberRegionRepository {

    private final MemberRegionJpaRepository memberRegionJpaRepository;

    @Override
    public MemberRegion save(MemberRegion memberRegion) {
        return memberRegionJpaRepository.save(memberRegion);
    }

    @Override
    public Optional<MemberRegion> findById(Long id) {
        return memberRegionJpaRepository.findById(id);
    }

    @Override
    public Optional<MemberRegion> findByMemberIdAndRegionId(Long memberId, Integer regionId) {
        return memberRegionJpaRepository.findFirstByMemberIdAndRegionId(memberId, regionId);
    }

    @Override
    public Optional<MemberRegion> findPrimaryByMemberIdAndRegionId(Long memberId, Integer regionId) {
        return memberRegionJpaRepository.findFirstByMemberIdAndRegionIdAndPrimaryTrue(memberId, regionId);
    }

    @Override
    public Optional<MemberRegion> findVerifiedPrimaryByMemberIdAndRegionId(Long memberId, Integer regionId) {
        return memberRegionJpaRepository.findFirstByMemberIdAndRegionIdAndVerifiedAtIsNotNullAndPrimaryTrue(memberId, regionId);
    }

    @Override
    public boolean existsPrimaryByMemberIdAndRegionId(Long memberId, Integer regionId) {
        return memberRegionJpaRepository.existsByMemberIdAndRegionIdAndPrimaryTrue(memberId, regionId);
    }

    @Override
    public long countPrimaryByMemberId(Long memberId) {
        return memberRegionJpaRepository.countByMemberIdAndPrimaryTrue(memberId);
    }
}
