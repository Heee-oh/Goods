package com.goods.market.chat.application.dto;

import java.time.Instant;

public record ChatRoomAppointmentDto(
        Long appointmentId,
        Instant meetAt,
        Integer reminderMinutes
) {
}
