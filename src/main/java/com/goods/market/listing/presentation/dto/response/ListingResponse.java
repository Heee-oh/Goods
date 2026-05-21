package com.goods.market.listing.presentation.dto.response;

import com.goods.market.listing.application.dto.ListingItemDto;
import com.goods.market.listing.domain.Status;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.Instant;

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
    public static ListingResponse from(ListingItemDto dto) {
        return new ListingResponse(
                dto.listingId(),
                dto.sellerId(),
                dto.title(),
                dto.categoryId(),
                dto.priceAmount(),
                dto.transactionType(),
                dto.status(),
                dto.dongnm(),
                dto.hopeRegionId(),
                dto.hopeLat(),
                dto.hopeLng(),
                dto.originRegionId(),
                dto.originLat(),
                dto.originLng(),
                dto.viewCount(),
                dto.chatCnt(),
                dto.firstImage(),
                dto.updatedAt(),
                dto.distanceKm()
        );
    }
}
