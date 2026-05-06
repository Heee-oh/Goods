package com.goods.market.trade.application.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record TradePromptResponse(
        Long appointmentId,
        Long listingId,
        Long chatRoomId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long buyerId,
        String partnerNickname
) {
}
