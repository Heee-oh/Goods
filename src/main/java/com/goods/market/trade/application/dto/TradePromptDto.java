package com.goods.market.trade.application.dto;

public record TradePromptDto(
        Long appointmentId,
        Long listingId,
        Long chatRoomId,
        Long buyerId,
        String partnerNickname,
        String listingTitle
) {
}
