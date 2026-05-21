package com.goods.market.chat.presentation.dto.response;

import com.goods.market.chat.application.dto.ChatRoomDetailDto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.util.List;

public record ChatRoomDetailResponse(
        Long chatRoomId,
        Long listingId,
        String listingFirstImage,
        String listingStatus,
        String listingTransactionType,
        Long sellerId,
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
    public static ChatRoomDetailResponse from(ChatRoomDetailDto dto) {
        return new ChatRoomDetailResponse(
                dto.chatRoomId(),
                dto.listingId(),
                dto.listingFirstImage(),
                dto.listingStatus(),
                dto.listingTransactionType(),
                dto.sellerId(),
                dto.partnerId(),
                dto.partnerNickname(),
                dto.partnerProfileImage(),
                dto.partnerSmileScore(),
                dto.listingTitle(),
                dto.listingPrice(),
                dto.currentAppointment() == null ? null : ChatRoomAppointmentResponse.from(dto.currentAppointment()),
                dto.messages().stream().map(ChatMessageItemResponse::from).toList()
        );
    }
}
