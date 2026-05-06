package com.goods.market.listing.application;

import com.goods.market.chat.infrastructure.ChatRoomRepository;
import com.goods.market.listing.application.dto.ListingResponse;
import com.goods.market.listing.infrastructure.ListingJpaRepository;
import com.goods.market.member.domain.MemberRegion;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
import com.goods.market.member.infrastructure.memberRegion.MemberRegionJpaRepository;
import com.goods.market.region.infrastructure.RegionJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingQueryServiceImplTest {

    @Mock
    private ListingJpaRepository listingJpaRepository;

    @Mock
    private MemberJpaRepository memberJpaRepository;

    @Mock
    private MemberRegionJpaRepository memberRegionJpaRepository;

    @Mock
    private RegionJpaRepository regionJpaRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @InjectMocks
    private ListingQueryServiceImpl listingQueryService;

    @Test
    void getListingsUsesMemberRegionCoordinatesAsOrigin() {
        Long memberId = 1L;
        Integer regionId = 11000;
        Long lastListingId = 99L;
        BigDecimal lat = new BigDecimal("37.5665");
        BigDecimal lng = new BigDecimal("126.9780");

        when(memberRegionJpaRepository.findFirstByMember_IdAndRegionIdAndVerifiedAtIsNotNull(memberId, regionId))
                .thenReturn(Optional.of(new MemberRegion(regionId, true, lat, lng)));
        Slice<ListingResponse> slice = new SliceImpl<>(List.of(), PageRequest.of(0, 20), false);
        when(listingJpaRepository.findListings(eq(memberId), eq(regionId), eq(lat), eq(lng), eq(lastListingId), isNull(), isNull(), eq(20), any(Pageable.class)))
                .thenReturn(slice);

        listingQueryService.getListings(memberId, regionId, lastListingId);

        verify(listingJpaRepository).findListings(eq(memberId), eq(regionId), eq(lat), eq(lng), eq(lastListingId), isNull(), isNull(), eq(20), any(Pageable.class));
    }

    @Test
    void getListingsFallsBackToNullOriginWhenMemberRegionHasNoCoordinates() {
        Long memberId = 1L;
        Integer regionId = 11000;
        Long lastListingId = 99L;

        when(memberRegionJpaRepository.findFirstByMember_IdAndRegionIdAndVerifiedAtIsNotNull(memberId, regionId))
                .thenReturn(Optional.of(new MemberRegion(regionId, true)));
        Slice<ListingResponse> slice = new SliceImpl<>(List.of(), PageRequest.of(0, 20), false);
        when(listingJpaRepository.findListings(eq(memberId), eq(regionId), isNull(), isNull(), eq(lastListingId), isNull(), isNull(), eq(20), any(Pageable.class)))
                .thenReturn(slice);

        listingQueryService.getListings(memberId, regionId, lastListingId);

        verify(listingJpaRepository).findListings(eq(memberId), eq(regionId), isNull(), isNull(), eq(lastListingId), isNull(), isNull(), eq(20), any(Pageable.class));
    }
}
