package com.goods.market.listing.application.dto;

import com.goods.market.listing.domain.HopeLocation;
import com.goods.market.listing.domain.Listing;
import com.goods.market.listing.domain.ListingImage;
import com.goods.market.listing.domain.Price;
import com.goods.market.listing.domain.TransactionType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.goods.market.member.domain.Member;

import java.math.BigDecimal;
import java.util.Comparator;
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
    public ListingDetailResponse(
            Long listingId,
            Long sellerId,
            String sellerNickname,
            String sellerProfileImage,
            int sellerSmileScore,
            Long buyerId,
            Long reserverId,
            String title,
            String description,
            Long categoryId,
            Long priceAmount,
            String transactionType,
            boolean interested,
            String regionName,
            long chatCount,
            boolean hidden,
            String status,
            Integer hopeRegionId,
            BigDecimal hopeLat,
            BigDecimal hopeLng,
            Double distanceKm,
            long viewCount,
            List<ListingImageResponse> images,
            java.time.Instant createdAt,
            java.time.Instant updatedAt
    ) {
        this(
                listingId,
                sellerId,
                buyerId,
                reserverId,
                sellerNickname,
                sellerProfileImage,
                sellerSmileScore,
                title,
                description,
                categoryId,
                priceAmount,
                transactionType,
                interested,
                regionName,
                chatCount,
                status,
                hopeRegionId,
                hopeLat,
                hopeLng,
                distanceKm,
                viewCount,
                images,
                updatedAt
        );
    }

    public static ListingDetailResponse from(
            Listing listing,
            Member seller,
            String regionName,
            long chatCount,
            boolean interested
            ,
            Double distanceKm
    ) {
        Price price = listing.getPrice();
        HopeLocation hopeLocation = listing.getHopeLocation();
        TransactionType resolvedTransactionType = price == null ? TransactionType.SELL : price.resolveTransactionType();

        List<ListingImageResponse> imageResponses = listing.getImages().stream()
                .sorted(Comparator.comparingInt(ListingImage::getSortOrder))
                .map(ListingImageResponse::from)
                .toList();

        return new ListingDetailResponse(
                listing.getId(),
                listing.getSellerId(),
                listing.getBuyerId(),
                listing.getReserverId(),
                seller.getNickname(),
                seller.getProfileImageUrl(),
                seller.getSmileScore(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getCategoryId(),
                price == null ? null : price.getPriceAmount(),
                resolvedTransactionType.getValue(),
                interested,
                regionName,
                chatCount,
                listing.getStatus().name(),
                hopeLocation == null ? null : hopeLocation.getRegionId(),
                hopeLocation == null ? null : hopeLocation.getLat(),
                hopeLocation == null ? null : hopeLocation.getLng(),
                distanceKm,
                listing.getViewCount(),
                imageResponses,
                listing.getUpdatedAt()
        );
    }
}

