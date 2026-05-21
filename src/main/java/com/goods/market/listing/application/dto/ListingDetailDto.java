package com.goods.market.listing.application.dto;

import com.goods.market.listing.domain.HopeLocation;
import com.goods.market.listing.domain.Listing;
import com.goods.market.listing.domain.ListingImage;
import com.goods.market.listing.domain.Price;
import com.goods.market.listing.domain.TransactionType;
import com.goods.market.member.domain.Member;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record ListingDetailDto(
        Long listingId,
        Long sellerId,
        Long buyerId,
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
        List<ListingImageDto> images,
        java.time.Instant updatedAt
) {
    public static ListingDetailDto from(
            Listing listing,
            Member seller,
            String regionName,
            long chatCount,
            boolean interested,
            Double distanceKm
    ) {
        Price price = listing.getPrice();
        HopeLocation hopeLocation = listing.getHopeLocation();
        TransactionType resolvedTransactionType = price == null ? TransactionType.SELL : price.resolveTransactionType();

        List<ListingImageDto> imageDtos = listing.getImages().stream()
                .sorted(Comparator.comparingInt(ListingImage::getSortOrder))
                .map(ListingImageDto::from)
                .toList();

        return new ListingDetailDto(
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
                imageDtos,
                listing.getUpdatedAt()
        );
    }
}
