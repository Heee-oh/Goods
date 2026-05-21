package com.goods.market.listing.presentation.dto.response;

import com.goods.market.listing.application.dto.ListingDetailDto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.util.List;

public record ListingDetailResponse(
        Long listingId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long sellerId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long buyerId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long reserverId,
        String sellerNickname,
        String sellerProfileImage,
        int sellerSmileScore,
        String title,
        String description,
        Long categoryId,
        Long priceAmount,
        String transactionType,
        boolean interested,
        String regionName,
        long chatCount,
        String status,
        Integer hopeRegionId,
        BigDecimal hopeLat,
        BigDecimal hopeLng,
        Double distanceKm,
        long viewCount,
        List<ListingImageResponse> images,
        java.time.Instant updatedAt
) {
    public static ListingDetailResponse from(ListingDetailDto dto) {
        return new ListingDetailResponse(
                dto.listingId(),
                dto.sellerId(),
                dto.buyerId(),
                dto.reserverId(),
                dto.sellerNickname(),
                dto.sellerProfileImage(),
                dto.sellerSmileScore(),
                dto.title(),
                dto.description(),
                dto.categoryId(),
                dto.priceAmount(),
                dto.transactionType(),
                dto.interested(),
                dto.regionName(),
                dto.chatCount(),
                dto.status(),
                dto.hopeRegionId(),
                dto.hopeLat(),
                dto.hopeLng(),
                dto.distanceKm(),
                dto.viewCount(),
                dto.images().stream().map(ListingImageResponse::from).toList(),
                dto.updatedAt()
        );
    }
}
