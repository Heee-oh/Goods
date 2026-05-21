package com.goods.market.chat.application.dto;

import java.time.Instant;

public record ChatRoomSummaryDto(
        Long chatRoomId,
        Long listingId,
        String listingTitle,
        Long listingPrice,
        String listingStatus,
        String listingTransactionType,
        String listingFirstImage,
        boolean sellerView,
        String partnerNickname,
        String partnerProfileImage,
        String regionName,
        String lastMessage,
        Instant lastMessageAt
) {
}
