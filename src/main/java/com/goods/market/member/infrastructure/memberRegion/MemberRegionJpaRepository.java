package com.goods.market.member.infrastructure.memberRegion;

import com.goods.market.member.domain.MemberRegion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface MemberRegionJpaRepository extends JpaRepository<MemberRegion, Long>, MemberRegionRepositoryCustom {

    Optional<MemberRegion> findMemberRegionByIdAndMemberId(Long id, Long memberId);

    Optional<MemberRegion> findFirstByMemberIdAndRegionIdAndVerifiedAtIsNotNull(Long memberId, Integer regionId);

    Optional<MemberRegion> findFirstByMemberIdAndRegionIdAndVerifiedAtIsNotNullAndPrimaryTrue(Long memberId, Integer regionId);

    Optional<MemberRegion> findFirstByMemberIdAndRegionIdAndPrimaryTrue(Long memberId, Integer regionId);

    Optional<MemberRegion> findFirstByMemberIdAndRegionId(Long memberId, Integer regionId);

    boolean existsByMemberIdAndRegionIdAndPrimaryTrue(Long memberId, Integer regionId);

    long countByMemberIdAndPrimaryTrue(Long memberId);

    void deleteByMemberId(Long memberId);
}
