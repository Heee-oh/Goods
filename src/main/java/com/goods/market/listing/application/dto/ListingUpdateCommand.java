package com.goods.market.listing.application.dto;

import com.goods.market.listing.domain.TransactionType;
import java.math.BigDecimal;
import java.util.List;

public record ListingUpdateCommand(
        String title,
        String description,
        Long categoryId,
        Long priceAmount,
        TransactionType transactionType,
        Integer hopeRegionId,
        BigDecimal hopeLat,
        BigDecimal hopeLng,
        List<String> imageUrls
) {

}
