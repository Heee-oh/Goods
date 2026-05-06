package com.goods.market.listing.application.dto;

import com.goods.market.listing.domain.Status;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.querydsl.core.annotations.QueryProjection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ListingResponse(
        Long listingId,
        @JsonSerialize(using = ToStringSerializer.class)
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
    public ListingResponse {
    }


}
