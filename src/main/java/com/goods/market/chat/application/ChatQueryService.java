package com.goods.market.chat.application;

import com.goods.market.chat.presentation.dto.ChatRoomDetailResponse;
import com.goods.market.chat.presentation.dto.ChatRoomSummaryResponse;

import java.util.List;

public interface ChatQueryService {

    List<ChatRoomSummaryResponse> getChatRooms(Long memberId);

    ChatRoomDetailResponse getChatRoom(Long memberId, Long chatRoomId);
}
