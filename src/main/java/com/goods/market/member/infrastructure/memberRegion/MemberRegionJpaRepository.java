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

    Optional<MemberRegion> findFirstByMember_IdAndRegionIdAndVerifiedAtIsNotNull(Long memberId, Integer regionId);

    Optional<MemberRegion> findFirstByMember_IdAndRegionIdAndVerifiedAtIsNotNullAndPrimaryTrue(Long memberId, Integer regionId);

    Optional<MemberRegion> findFirstByMember_IdAndRegionIdAndPrimaryTrue(Long memberId, Integer regionId);

    Optional<MemberRegion> findFirstByMember_IdAndRegionId(Long memberId, Integer regionId);

    boolean existsByMember_IdAndRegionIdAndPrimaryTrue(Long memberId, Integer regionId);

    long countByMember_IdAndPrimaryTrue(Long memberId);

    void deleteByMember_Id(Long memberId);
}
