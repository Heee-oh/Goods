package com.goods.market.chat.application;

import com.goods.market.chat.application.dto.ChatRoomDetailDto;
import com.goods.market.chat.application.dto.ChatRoomSummaryDto;

import java.util.List;

public interface ChatQueryService {

    List<ChatRoomSummaryDto> getChatRooms(Long memberId);

    ChatRoomDetailDto getChatRoom(Long memberId, Long chatRoomId);
}
