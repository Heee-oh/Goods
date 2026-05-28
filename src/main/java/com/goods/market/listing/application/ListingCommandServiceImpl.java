package com.goods.market.listing.application;

import com.goods.market.listing.application.dto.ListingUpdateCommand;
import com.goods.market.listing.domain.HopeLocation;
import com.goods.market.listing.domain.Listing;
import com.goods.market.listing.domain.ListingImage;
import com.goods.market.listing.exception.ListingAccessDeniedException;
import com.goods.market.listing.exception.ListingBadRequestException;
import com.goods.market.listing.exception.ListingNotFoundException;
import com.goods.market.listing.infrastructure.ListingJpaRepository;
import com.goods.market.member.domain.MemberRegion;
import com.goods.market.member.infrastructure.memberRegion.MemberRegionJpaRepository;
import com.goods.market.common.event.DomainEventPublisher;
import com.goods.market.common.event.events.ListingCreatedEvent;
import com.goods.market.common.event.events.ListingReservationCanceledEvent;
import com.goods.market.common.event.events.ListingSoldOutEvent;
import com.goods.market.trade.infrastructure.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class ListingCommandServiceImpl implements ListingCommandService {

    private final ListingJpaRepository listingJpaRepository;
    private final MemberRegionJpaRepository memberRegionJpaRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final TradeRepository tradeRepository;

    @Override
    public Long createDraft(Long sellerId, Integer regionId) {
        MemberRegion originRegion = memberRegionJpaRepository
                .findFirstByMember_IdAndRegionIdAndVerifiedAtIsNotNull(sellerId, regionId)
                .orElse(null);

        Integer originRegionId = null;
        java.math.BigDecimal originLat = null;
        java.math.BigDecimal originLng = null;

        if (originRegion != null && originRegion.getLat() != null && originRegion.getLng() != null) {
            originRegionId = originRegion.getRegionId();
            originLat = originRegion.getLat();
            originLng = originRegion.getLng();
        }

        Listing listing = Listing.createEmptyDraft(
                sellerId,
                regionId,
                originRegionId,
                originLat,
                originLng
        );
        return listingJpaRepository.save(listing).getId();
    }

    /**
     * 작성 중인 글의 내용을 저장한다.
     */
    @Override
    public void update(Long sellerId, Long listingId, ListingUpdateCommand command) {
        Listing listing = findActiveWithImages(listingId);
        validateOwnership(listing, sellerId);

        listing.updateTitleAndDescription(command.title(), command.description());
        listing.updateCategory(command.categoryId());
        listing.updatePrice(command.priceAmount(), command.transactionType());
        listing.updateHopeLocation(toHopeLocation(command.hopeRegionId(), command.hopeLat(), command.hopeLng()));
        listing.replaceImages(toListingImages(command.imageUrls()));
    }

    /**
     * 글 상태를 DRAFT에서 PUBLISHED로 전환한다.
     */
    @Override
    public void publish(Long sellerId, Long listingId) {
        Listing listing = findActive(listingId);
        validateOwnership(listing, sellerId);
        listing.publish();
        // 게시글 등록 이벤트를 발행한다.
        domainEventPublisher.publish(new ListingCreatedEvent(
                listing.getId(),
                listing.getSellerId(),
                listing.getRegionId(),
                listing.getTitle()
        ));
    }

    /**
     * 게시 중인 글을 숨김 처리한다.
     */
    @Override
    public void hide(Long sellerId, Long listingId) {
        Listing listing = findActive(listingId);
        validateOwnership(listing, sellerId);
        listing.hide();
    }

    /**
     * 숨김 처리된 글을 다시 노출한다.
     */
    @Override
    public void unhide(Long sellerId, Long listingId) {
        Listing listing = findActive(listingId);
        validateOwnership(listing, sellerId);
        listing.unHide();
    }

    /**
     * 글에 예약자를 지정한다.
     */
    @Override
    public void reserve(Long sellerId, Long listingId, Long reserverId) {
        Listing listing = findActive(listingId);
        validateOwnership(listing, sellerId);
        listing.reserve(reserverId);
    }

    /**
     * 예약을 취소하고 게시 상태로 되돌린다.
     */
    @Override
    public void cancelReserve(Long sellerId, Long listingId) {
        Listing listing = findActive(listingId);
        validateOwnership(listing, sellerId);
        Long reserverId = listing.getReserverId();
        listing.cancelReserve();
        // 예약 취소 이벤트를 발행한다.
        domainEventPublisher.publish(new ListingReservationCanceledEvent(
                listing.getId(),
                reserverId
        ));
    }

    /**
     * 구매자를 확정하고 판매 완료 처리한다.
     */
    @Override
    public Long markSoldOut(Long sellerId, Long listingId, Long buyerId) {
        Listing listing = findActive(listingId);
        validateOwnership(listing, sellerId);
        listing.markSoldOut(buyerId);
        // 판매 완료 이벤트를 발행한다.
        domainEventPublisher.publish(new ListingSoldOutEvent(
                listing.getId(),
                listing.getSellerId(),
                listing.getBuyerId(),
                listing.getPrice()
        ));
        return tradeRepository.findByListingId(listing.getId())
                .map(com.goods.market.trade.domain.Trade::getId)
                .orElseThrow(() -> new IllegalStateException("Trade was not created for sold out listing."));
    }

    /**
     * 글을 소프트 삭제한다.
     */
    @Override
    public void remove(Long sellerId, Long listingId) {
        Listing listing = findActive(listingId);
        validateOwnership(listing, sellerId);
        listing.remove();
    }

    /**
     * 삭제되지 않은 글을 조회한다.
     */
    private Listing findActive(Long listingId) {
        return listingJpaRepository.findByIdAndDeletedAtIsNull(listingId)
                .orElseThrow(ListingNotFoundException::new);
    }

    /**
     * 수정 흐름에서 사용하도록 이미지와 함께 글을 조회한다.
     */
    private Listing findActiveWithImages(Long listingId) {
        return listingJpaRepository.findActiveByIdWithImages(listingId)
                .orElseThrow(ListingNotFoundException::new);
    }

    /**
     * 현재 사용자가 글 작성자인지 확인한다.
     */
    private void validateOwnership(Listing listing, Long sellerId) {
        if (!listing.isOwnedBy(sellerId)) {
            throw new ListingAccessDeniedException();
        }
    }

    /**
     * 위치 입력값을 HopeLocation 값 객체로 변환한다.
     */
    private HopeLocation toHopeLocation(Integer regionId, BigDecimal lat, BigDecimal lng) {
        if (regionId == null && lat == null && lng == null) {
            return null;
        }
        if (regionId == null || lat == null || lng == null) {
            throw new ListingBadRequestException("hopeLocation requires regionId, lat and lng");
        }

        return new HopeLocation(regionId, lat, lng);
    }

    /**
     * 이미지 URL 목록을 ListingImage 도메인 객체로 변환한다.
     */
    private List<ListingImage> toListingImages(List<String> imageUrls) {
        if (imageUrls == null) {
            return List.of();
        }
        return imageUrls.stream()
                .map(ListingImage::of)
                .toList();
    }
}
