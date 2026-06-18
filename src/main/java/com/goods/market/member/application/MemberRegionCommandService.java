package com.goods.market.member.application;

import com.goods.market.member.domain.MemberRegion;
import com.goods.market.member.domain.MemberRegionRepository;
import com.goods.market.member.domain.MemberRepository;
import com.goods.market.member.domain.exception.MemberNotFoundException;
import com.goods.market.member.domain.exception.memberRegion.MemberRegionAlreadyExistsException;
import com.goods.market.member.domain.exception.memberRegion.MemberRegionLimitExceededException;
import com.goods.market.member.domain.exception.memberRegion.MemberRegionNotFoundException;
import com.goods.market.member.domain.exception.memberRegion.MemberRegionVerificationFailedException;
import com.goods.market.region.infrastructure.RegionJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberRegionCommandService {

    private final MemberRepository memberRepository;
    private final MemberRegionRepository memberRegionRepository;
    private final RegionJpaRepository regionJpaRepository;

    public void addMemberRegion(Integer regionId, Long memberId) {
        if (memberRegionRepository.existsPrimaryByMemberIdAndRegionId(memberId, regionId)) {
            throw new MemberRegionAlreadyExistsException();
        }

        if (memberRegionRepository.countPrimaryByMemberId(memberId) >= 2) {
            throw new MemberRegionLimitExceededException();
        }

        memberRegionRepository.findByMemberIdAndRegionId(memberId, regionId)
                .ifPresentOrElse(MemberRegion::reactivateUnverified, () -> {
                    memberRepository.findById(memberId)
                            .orElseThrow(MemberNotFoundException::new);

                    MemberRegion memberRegion = MemberRegion.unverified(regionId, true);
                    memberRegion.updateMember(memberId);
                    memberRegionRepository.save(memberRegion);
                });
    }

    public void verifyMemberRegionByRegionId(Integer regionId, Long memberId, BigDecimal lat, BigDecimal lng) {
        if (!regionJpaRepository.validateCoordinateInRegion(regionId, lat, lng)) {
            throw new MemberRegionVerificationFailedException();
        }

        // 멤버 존재 여부 확인
        memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        Optional<MemberRegion> activeRegion = memberRegionRepository.findPrimaryByMemberIdAndRegionId(memberId, regionId);
        if (activeRegion.isPresent()) {
            verify(activeRegion.get(), lat, lng);
            return;
        }

        Optional<MemberRegion> inactiveRegion = memberRegionRepository.findByMemberIdAndRegionId(memberId, regionId);
        if (inactiveRegion.isPresent()) {
            verify(inactiveRegion.get(), lat, lng);
            return;
        }



        MemberRegion memberRegion = new MemberRegion(regionId, true, lat, lng);
        memberRegion.updateMember(memberId);
        memberRegionRepository.save(memberRegion);

    }
    public void removeMemberRegion(Integer regionId, Long memberId) {
        MemberRegion memberRegion = memberRegionRepository
                .findPrimaryByMemberIdAndRegionId(memberId, regionId)
                .orElse(null);

        if (memberRegion == null) {
            return;
        }

        memberRegion.unsetPrimary();
    }

    private void verify(MemberRegion memberRegion, BigDecimal lat, BigDecimal lng) {
        if (memberRegion.getRegionId() == null) {
            throw new MemberRegionNotFoundException();
        }

        memberRegion.verify(Instant.now(), lat, lng);
    }
}
