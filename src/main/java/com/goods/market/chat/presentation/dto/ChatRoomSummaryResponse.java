package com.goods.market.chat.presentation.dto;

import java.time.Instant;

public record ChatRoomSummaryResponse(
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
