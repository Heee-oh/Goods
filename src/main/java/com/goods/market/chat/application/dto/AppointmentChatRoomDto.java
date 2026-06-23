package com.goods.market.chat.application.dto;

import com.goods.market.chat.domain.ChatRoom;

public record AppointmentChatRoomDto(
        Long chatRoomId,
        Long listingId,
        Long sellerId,
        Long buyerId
) {
    public static AppointmentChatRoomDto from(ChatRoom chatRoom) {
        return new AppointmentChatRoomDto(
                chatRoom.getId(),
                chatRoom.getListingId(),
                chatRoom.getSellerId(),
                chatRoom.getBuyerId()
        );
    }
}
