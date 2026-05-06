package com.goods.market.chat.infrastructure;

import com.goods.market.chat.presentation.dto.ChatRoomSummaryResponse;

import java.util.List;

public interface ChatRoomRepositoryCustom {

    List<ChatRoomSummaryResponse> findSummariesByMemberId(Long memberId);
}
