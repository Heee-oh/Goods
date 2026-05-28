package com.goods.market.listing.application;

import com.goods.market.listing.application.dto.ListingDetailDto;
import com.goods.market.listing.application.dto.ListingItemDto;
import com.goods.market.listing.domain.Listing;
import com.goods.market.listing.exception.ListingNotFoundException;
import com.goods.market.listing.infrastructure.ListingJpaRepository;
import com.goods.market.chat.domain.ChatRoomStatus;
import com.goods.market.chat.infrastructure.ChatRoomRepository;
import com.goods.market.member.domain.Member;
import com.goods.market.member.domain.MemberRegion;
import com.goods.market.member.domain.exception.MemberNotFoundException;
import com.goods.market.member.infrastructure.Interest.InterestJpaRepository;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
import com.goods.market.member.infrastructure.memberRegion.MemberRegionJpaRepository;
import com.goods.market.region.domain.Region;
import com.goods.market.region.infrastructure.RegionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListingQueryServiceImpl implements ListingQueryService {

    private final ListingJpaRepository listingJpaRepository;
    private final MemberJpaRepository memberJpaRepository;
    private final MemberRegionJpaRepository memberRegionJpaRepository;
    private final InterestJpaRepository interestJpaRepository;
    private final RegionJpaRepository regionJpaRepository;
    private final ChatRoomRepository chatRoomRepository;

    /**
     * 삭제되지 않은 글 상세를 조회한다.
     */
    @Override
    public ListingDetailDto getListing(Long listingId, Long viewerMemberId, Integer regionId) {
        Listing listing = listingJpaRepository.findActiveByIdWithImages(listingId)
                .orElseThrow(ListingNotFoundException::new);

        Member seller = memberJpaRepository.findById(listing.getSellerId())
                .orElseThrow(MemberNotFoundException::new);

        String regionName = null;
        if (listing.getRegionId() != null) {
            regionName = regionJpaRepository.findById(listing.getRegionId())
                    .map(Region::getDongnm)
                    .orElse(null);
        }
        long chatCount = chatRoomRepository.countByListingIdAndStatus(listingId, ChatRoomStatus.ACTIVE);
        boolean interested = viewerMemberId != null
                && interestJpaRepository.existsByListingIdAndMemberId(listingId, viewerMemberId);
        Double distanceKm = resolveDistance(viewerMemberId, regionId, listing);

        return ListingDetailDto.from(listing, seller, regionName, chatCount, interested, distanceKm);
    }

    @Override
    public Slice<ListingItemDto> getListings(Long memberId, Integer regionId, Long lastListingId, String transactionType, Long sellerId) {
        PageRequest pageRequest = PageRequest.of(0, 20);

        MemberRegion originRegion = memberRegionJpaRepository
                .findFirstByMember_IdAndRegionIdAndVerifiedAtIsNotNull(memberId, regionId)
                .orElse(null);

        BigDecimal originLat = originRegion != null ? originRegion.getLat() : null;
        BigDecimal originLng = originRegion != null ? originRegion.getLng() : null;

        return listingJpaRepository.findListings(
                memberId,
                regionId,
                originLat,
                originLng,
                lastListingId,
                transactionType,
                sellerId,
                20,
                pageRequest
        );
    }

    private Double resolveDistance(Long viewerMemberId, Integer regionId, Listing listing) {
        if (viewerMemberId == null || regionId == null) {
            return null;
        }

        MemberRegion originRegion = memberRegionJpaRepository
                .findFirstByMember_IdAndRegionIdAndVerifiedAtIsNotNull(viewerMemberId, regionId)
                .orElse(null);

        if (originRegion == null || originRegion.getLat() == null || originRegion.getLng() == null) {
            return null;
        }

        java.math.BigDecimal listingLat = listing.getOriginLat() != null ? listing.getOriginLat()
                : listing.getHopeLocation() == null ? null : listing.getHopeLocation().getLat();
        java.math.BigDecimal listingLng = listing.getOriginLng() != null ? listing.getOriginLng()
                : listing.getHopeLocation() == null ? null : listing.getHopeLocation().getLng();

        if (listingLat == null || listingLng == null) {
            return null;
        }

        double originLat = originRegion.getLat().doubleValue();
        double originLng = originRegion.getLng().doubleValue();
        double targetLat = listingLat.doubleValue();
        double targetLng = listingLng.doubleValue();

        double radians = Math.acos(Math.min(1d, Math.max(-1d,
                Math.cos(Math.toRadians(originLat)) * Math.cos(Math.toRadians(targetLat))
                        * Math.cos(Math.toRadians(targetLng) - Math.toRadians(originLng))
                        + Math.sin(Math.toRadians(originLat)) * Math.sin(Math.toRadians(targetLat))
        )));

        return 6371.0d * radians;
    }
}
