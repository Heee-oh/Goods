package com.goods.market.trade.application;

import com.goods.market.trade.application.dto.AppointmentDto;
import com.goods.market.trade.application.dto.TradePromptDto;
import java.util.Optional;

public interface AppointmentCommandService {

    AppointmentDto schedule(Long memberId, Long chatRoomId, java.time.Instant meetAt, Integer reminderMinutes);

    void cancel(Long memberId, Long appointmentId);

    Optional<TradePromptDto> getTradePrompt(Long memberId);

    void dismissTradePrompt(Long memberId, Long appointmentId);

    void processDueNotifications();
}
