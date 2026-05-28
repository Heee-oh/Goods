package com.goods.market.listing.application.dto;

import com.goods.market.listing.domain.Status;
import com.querydsl.core.annotations.QueryProjection;

import java.math.BigDecimal;
import java.time.Instant;

public record ListingItemDto(
        Long listingId,
        Long sellerId,
        String title,
        Long categoryId,
        Long priceAmount,
        String transactionType,
        Status status,
        String dongnm,
        Integer hopeRegionId,
        BigDecimal hopeLat,
        BigDecimal hopeLng,
        Integer originRegionId,
        BigDecimal originLat,
        BigDecimal originLng,
        Long viewCount,
        Long chatCnt,
        String firstImage,
        Instant updatedAt,
        Double distanceKm
) {
    @QueryProjection
    public ListingItemDto {
    }
}
