package com.goods.market.listing.application;

import com.goods.market.listing.application.dto.ListingDetailDto;
import com.goods.market.listing.application.dto.ListingItemDto;
import org.springframework.data.domain.Slice;

public interface ListingQueryService {
    default ListingDetailDto getListing(Long listingId, Long viewerMemberId) {
        return getListing(listingId, viewerMemberId, null);
    }

    ListingDetailDto getListing(Long listingId, Long viewerMemberId, Integer regionId);
    default Slice<ListingItemDto> getListings(Long memberId, Integer regionId, Long lastListingId) {
        return getListings(memberId, regionId, lastListingId, null, null);
    }

    Slice<ListingItemDto> getListings(Long memberId, Integer regionId, Long lastListingId, String transactionType, Long sellerId);
}
