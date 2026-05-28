package com.goods.market.trade.application.dto;

import java.time.Instant;

public record AppointmentDto(
        Long appointmentId,
        Instant meetAt,
        Integer reminderMinutes
) {
}
