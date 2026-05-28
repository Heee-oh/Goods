package com.goods.market.chat.infrastructure;

import com.goods.market.chat.application.dto.ChatRoomSummaryDto;

import java.util.List;

public interface ChatRoomRepositoryCustom {

    List<ChatRoomSummaryDto> findSummariesByMemberId(Long memberId);
}
