package com.goods.market.listing.application;

import com.goods.market.listing.application.dto.ListingDetailResponse;
import com.goods.market.listing.application.dto.ListingResponse;
import com.goods.market.member.application.dto.InterestResponse;
import org.springframework.data.domain.Slice;

public interface ListingQueryService {
    default ListingDetailResponse getListing(Long listingId, Long viewerMemberId) {
        return getListing(listingId, viewerMemberId, null);
    }

    ListingDetailResponse getListing(Long listingId, Long viewerMemberId, Integer regionId);
    default Slice<ListingResponse> getListings(Long memberId, Integer regionId, Long lastListingId) {
        return getListings(memberId, regionId, lastListingId, null, null);
    }

    Slice<ListingResponse> getListings(Long memberId, Integer regionId, Long lastListingId, String transactionType, Long sellerId);
}

