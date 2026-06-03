package com.goods.market.member.application;

import com.goods.market.member.domain.MemberRegion;
import com.goods.market.member.domain.Member;
import com.goods.market.member.domain.exception.memberRegion.MemberRegionVerificationFailedException;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
import com.goods.market.member.infrastructure.memberRegion.MemberRegionJpaRepository;
import com.goods.market.region.infrastructure.RegionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberRegionCommandServiceImplTest {

    @Mock
    MemberJpaRepository memberJpaRepository;

    @Mock
    MemberRegionJpaRepository memberRegionJpaRepository;

    @Mock
    RegionJpaRepository regionJpaRepository;

    @InjectMocks
    MemberRegionCommandServiceImpl service;

    @Test
    @DisplayName("기존 동네가 없으면 검증 완료 시 좌표를 저장한 새 memberRegion을 추가한다")
    void verifyMemberRegionByRegionId_addsVerifiedRegionWithCoordinates_whenRegionDoesNotExist() {
        Long memberId = 10L;
        Integer regionId = 11000;
        BigDecimal lat = new BigDecimal("37.5665");
        BigDecimal lng = new BigDecimal("126.9780");

        Member member = org.mockito.Mockito.mock(Member.class);
        when(memberJpaRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberRegionJpaRepository.findFirstByMember_IdAndRegionIdAndPrimaryTrue(memberId, regionId))
                .thenReturn(Optional.empty());
        when(memberRegionJpaRepository.findFirstByMember_IdAndRegionId(memberId, regionId)).thenReturn(Optional.empty());
        when(regionJpaRepository.validateCoordinateInRegion(regionId, lat, lng)).thenReturn(true);

        service.verifyMemberRegionByRegionId(regionId, memberId, lat, lng);

        var regionCaptor = org.mockito.ArgumentCaptor.forClass(MemberRegion.class);
        verify(member).addRegion(regionCaptor.capture());
        MemberRegion savedRegion = regionCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(savedRegion.getRegionId()).isEqualTo(regionId);
        org.assertj.core.api.Assertions.assertThat(savedRegion.getLat()).isEqualTo(lat);
        org.assertj.core.api.Assertions.assertThat(savedRegion.getLng()).isEqualTo(lng);
        org.assertj.core.api.Assertions.assertThat(savedRegion.isVerified()).isTrue();
    }

    @Test
    @DisplayName("동네 삭제 요청은 실제 삭제 대신 primary를 false로 바꾼다")
    void removeMemberRegion_unsetsPrimary_whenActiveRegionExists() {
        Long memberId = 10L;
        Integer regionId = 11000;

        MemberRegion memberRegion = org.mockito.Mockito.mock(MemberRegion.class);
        when(memberRegionJpaRepository.findFirstByMember_IdAndRegionIdAndPrimaryTrue(memberId, regionId))
                .thenReturn(Optional.of(memberRegion));

        service.removeMemberRegion(regionId, memberId);

        verify(memberRegion).unsetPrimary();
    }

    @Test
    @DisplayName("동네 추가는 primary=true인 활성 동네만 개수 제한에 반영한다")
    void addMemberRegion_countsOnlyActiveRegions() {
        Long memberId = 10L;
        Integer regionId = 11000;

        Member member = org.mockito.Mockito.mock(Member.class);
        when(memberJpaRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberRegionJpaRepository.existsByMember_IdAndRegionIdAndPrimaryTrue(memberId, regionId))
                .thenReturn(false);
        when(memberRegionJpaRepository.countByMember_IdAndPrimaryTrue(memberId)).thenReturn(1L);
        when(memberRegionJpaRepository.findFirstByMember_IdAndRegionId(memberId, regionId))
                .thenReturn(Optional.empty());

        service.addMemberRegion(regionId, memberId);

        var regionCaptor = org.mockito.ArgumentCaptor.forClass(MemberRegion.class);
        verify(member).addRegion(regionCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(regionCaptor.getValue().isVerified()).isFalse();
        org.assertj.core.api.Assertions.assertThat(regionCaptor.getValue().getRegionId()).isEqualTo(regionId);
    }

    @Test
    @DisplayName("비활성 memberRegion이 있으면 verify 시 다시 활성화한다")
    void verifyMemberRegionByRegionId_reactivatesExistingRegion_whenInactiveRegionExists() {
        Long memberId = 10L;
        Integer regionId = 11000;
        BigDecimal lat = new BigDecimal("37.5665");
        BigDecimal lng = new BigDecimal("126.9780");

        MemberRegion memberRegion = org.mockito.Mockito.mock(MemberRegion.class);
        when(memberRegion.getRegionId()).thenReturn(regionId);
        when(memberRegionJpaRepository.findFirstByMember_IdAndRegionIdAndPrimaryTrue(memberId, regionId))
                .thenReturn(Optional.empty());
        when(memberRegionJpaRepository.findFirstByMember_IdAndRegionId(memberId, regionId))
                .thenReturn(Optional.of(memberRegion));
        when(regionJpaRepository.validateCoordinateInRegion(regionId, lat, lng)).thenReturn(true);

        service.verifyMemberRegionByRegionId(regionId, memberId, lat, lng);

        verify(regionJpaRepository).validateCoordinateInRegion(regionId, lat, lng);
        verifyNoMoreInteractions(regionJpaRepository);
        verify(memberRegion).verify(any(Instant.class), eq(lat), eq(lng));
    }

    @Test
    @DisplayName("비활성 memberRegion이 있으면 add 시 다시 활성화한다")
    void addMemberRegion_reactivatesExistingRegion_whenInactiveRegionExists() {
        Long memberId = 10L;
        Integer regionId = 11000;

        MemberRegion memberRegion = org.mockito.Mockito.mock(MemberRegion.class);

        when(memberRegionJpaRepository.existsByMember_IdAndRegionIdAndPrimaryTrue(memberId, regionId))
                .thenReturn(false);
        when(memberRegionJpaRepository.countByMember_IdAndPrimaryTrue(memberId)).thenReturn(1L);
        when(memberRegionJpaRepository.findFirstByMember_IdAndRegionId(memberId, regionId))
                .thenReturn(Optional.of(memberRegion));

        service.addMemberRegion(regionId, memberId);

        verify(memberRegion).reactivateUnverified();
        verifyNoInteractions(memberJpaRepository);
    }

    @Test
    @DisplayName("좌표가 region에 포함되지 않으면 MemberRegionVerificationFailedException을 던진다")
    void verifyMemberRegionByRegionId_throwsVerificationFailed_whenCoordinateIsNotInRegion() {
        Long memberId = 10L;
        Integer regionId = 11000;
        BigDecimal lat = new BigDecimal("35.1796");
        BigDecimal lng = new BigDecimal("129.0756");

        when(regionJpaRepository.validateCoordinateInRegion(regionId, lat, lng))
                .thenReturn(false);

        assertThatThrownBy(() ->
                service.verifyMemberRegionByRegionId(regionId, memberId, lat, lng)
        ).isInstanceOf(MemberRegionVerificationFailedException.class);

        verify(regionJpaRepository).validateCoordinateInRegion(regionId, lat, lng);
        verifyNoInteractions(memberRegionJpaRepository, memberJpaRepository);
    }
}
