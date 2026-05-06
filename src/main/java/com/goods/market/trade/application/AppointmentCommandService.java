package com.goods.market.trade.application;

import com.goods.market.trade.application.dto.AppointmentResponse;
import com.goods.market.trade.application.dto.TradePromptResponse;
import java.util.Optional;

public interface AppointmentCommandService {

    AppointmentResponse schedule(Long memberId, Long chatRoomId, java.time.Instant meetAt, Integer reminderMinutes);

    void cancel(Long memberId, Long appointmentId);

    Optional<TradePromptResponse> getTradePrompt(Long memberId);

    void dismissTradePrompt(Long memberId, Long appointmentId);

    void processDueNotifications();
}
