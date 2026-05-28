package com.goods.market.chat.presentation.dto.response;

import com.goods.market.chat.application.dto.ChatRoomSummaryDto;

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
    public static ChatRoomSummaryResponse from(ChatRoomSummaryDto dto) {
        return new ChatRoomSummaryResponse(
                dto.chatRoomId(),
                dto.listingId(),
                dto.listingTitle(),
                dto.listingPrice(),
                dto.listingStatus(),
                dto.listingTransactionType(),
                dto.listingFirstImage(),
                dto.sellerView(),
                dto.partnerNickname(),
                dto.partnerProfileImage(),
                dto.regionName(),
                dto.lastMessage(),
                dto.lastMessageAt()
        );
    }
}
