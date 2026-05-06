package com.goods.market.chat.presentation.dto;

import java.time.Instant;

public record ChatRoomAppointmentResponse(
        Long appointmentId,
        Instant meetAt,
        Integer reminderMinutes
) {
}
