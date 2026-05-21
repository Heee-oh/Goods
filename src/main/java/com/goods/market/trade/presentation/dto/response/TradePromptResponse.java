package com.goods.market.trade.presentation.dto.response;

import com.goods.market.trade.application.dto.TradePromptDto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record TradePromptResponse(
        Long appointmentId,
        Long listingId,
        Long chatRoomId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long buyerId,
        String partnerNickname,
        String listingTitle
) {
    public static TradePromptResponse from(TradePromptDto dto) {
        return new TradePromptResponse(
                dto.appointmentId(),
                dto.listingId(),
                dto.chatRoomId(),
                dto.buyerId(),
                dto.partnerNickname(),
                dto.listingTitle()
        );
    }
}
