package com.goods.market.listing.infrastructure;

import com.goods.market.listing.application.dto.ListingItemDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ListingJpaRepositoryCustom {

    Slice<ListingItemDto> findListings(
            Long memberId,
            Integer regionId,
            java.math.BigDecimal originLat,
            java.math.BigDecimal originLng,
            Long lastListingId,
            String transactionType,
            Long sellerId,
            int size,
            Pageable pageable
    );

}
