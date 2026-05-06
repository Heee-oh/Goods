package com.goods.market.trade.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentScheduler {

    private final AppointmentCommandService appointmentCommandService;

    /**
     * 약속 알림과 거래 완료 유도 시점을 주기적으로 확인한다.
     */
    @Scheduled(fixedDelay = 60_000L)
    public void processDueNotifications() {
        appointmentCommandService.processDueNotifications();
    }
}
