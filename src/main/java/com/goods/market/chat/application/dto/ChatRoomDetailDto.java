package com.goods.market.chat.application.dto;

import java.util.List;

public record ChatRoomDetailDto(
        Long chatRoomId,
        Long listingId,
        String listingFirstImage,
        String listingStatus,
        String listingTransactionType,
        Long sellerId,
        Long partnerId,
        String partnerNickname,
        String partnerProfileImage,
        Integer partnerSmileScore,
        String listingTitle,
        Long listingPrice,
        ChatRoomAppointmentDto currentAppointment,
        List<ChatMessageItemDto> messages
) {
}
