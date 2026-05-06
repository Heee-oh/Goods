package com.goods.market.chat.presentation.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.util.List;

public record ChatRoomDetailResponse(
        Long chatRoomId,
        Long listingId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long partnerId,
        String partnerNickname,
        String partnerProfileImage,
        Integer partnerSmileScore,
        String listingTitle,
        Long listingPrice,
        ChatRoomAppointmentResponse currentAppointment,
        List<ChatMessageItemResponse> messages
) {
}
