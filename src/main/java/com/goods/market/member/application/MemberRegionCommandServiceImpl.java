package com.goods.market.member.application;

import com.goods.market.member.domain.Member;
import com.goods.market.member.domain.MemberRegion;
import com.goods.market.member.domain.exception.MemberNotFoundException;
import com.goods.market.member.domain.exception.memberRegion.MemberRegionAlreadyExistsException;
import com.goods.market.member.domain.exception.memberRegion.MemberRegionLimitExceededException;
import com.goods.market.member.domain.exception.memberRegion.MemberRegionNotFoundException;
import com.goods.market.member.domain.exception.memberRegion.MemberRegionVerificationFailedException;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
import com.goods.market.member.infrastructure.memberRegion.MemberRegionJpaRepository;
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
public class MemberRegionCommandServiceImpl implements MemberRegionCommandService {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberRegionJpaRepository memberRegionJpaRepository;
    private final RegionJpaRepository regionJpaRepository;

    @Override
    public void addMemberRegion(Integer regionId, Long memberId) {
        if (memberRegionJpaRepository.existsByMember_IdAndRegionIdAndPrimaryTrue(memberId, regionId)) {
            throw new MemberRegionAlreadyExistsException();
        }

        if (memberRegionJpaRepository.countByMember_IdAndPrimaryTrue(memberId) >= 2) {
            throw new MemberRegionLimitExceededException();
        }

        memberRegionJpaRepository.findFirstByMember_IdAndRegionId(memberId, regionId)
                .ifPresentOrElse(MemberRegion::reactivateUnverified, () -> {
                    Member member = memberJpaRepository.findById(memberId)
                            .orElseThrow(MemberNotFoundException::new);

                    member.addRegion(MemberRegion.unverified(regionId, true));
                });
    }

    @Override
    public void verifyMemberRegion(Long memberRegionId, Long memberId, BigDecimal lat, BigDecimal lng) {
        MemberRegion memberRegion = memberRegionJpaRepository.findMemberRegionByIdAndMemberId(memberRegionId, memberId)
                .orElseThrow(MemberRegionNotFoundException::new);

        Integer regionId = memberRegion.getRegionId();
        if (regionId == null) {
            throw new MemberRegionNotFoundException();
        }

        verify(memberRegion, lat, lng);
    }

    @Override
    public void verifyMemberRegionByRegionId(Integer regionId, Long memberId, BigDecimal lat, BigDecimal lng) {
        if (!regionJpaRepository.validateCoordinateInRegion(regionId, lat, lng)) {
            throw new MemberRegionVerificationFailedException();
        }

        Optional<MemberRegion> activeRegion = memberRegionJpaRepository
                .findFirstByMember_IdAndRegionIdAndPrimaryTrue(memberId, regionId);
        if (activeRegion.isPresent()) {
            verify(activeRegion.get(), lat, lng);
            return;
        }

        Optional<MemberRegion> inactiveRegion = memberRegionJpaRepository
                .findFirstByMember_IdAndRegionId(memberId, regionId);
        if (inactiveRegion.isPresent()) {
            verify(inactiveRegion.get(), lat, lng);
            return;
        }

        Member member = memberJpaRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        member.addRegion(new MemberRegion(regionId, true, lat, lng));
    }

    @Override
    public void removeMemberRegion(Integer regionId, Long memberId) {
        MemberRegion memberRegion = memberRegionJpaRepository
                .findFirstByMember_IdAndRegionIdAndPrimaryTrue(memberId, regionId)
                .orElse(null);

        if (memberRegion == null) {
            return;
        }

        memberRegion.unsetPrimary();
    }

    private void verify(MemberRegion memberRegion, BigDecimal lat, BigDecimal lng) {
        Integer regionId = memberRegion.getRegionId();

        if (regionId == null) {
            throw new MemberRegionNotFoundException();
        }

        if (!regionJpaRepository.validateCoordinateInRegion(regionId, lat, lng)) {
            throw new MemberRegionVerificationFailedException();
        }

        memberRegion.verify(Instant.now(), lat, lng);
    }
}
