package com.goods.market.trade.presentation.dto.response;

import com.goods.market.trade.application.dto.AppointmentDto;

import java.time.Instant;

public record AppointmentResponse(
        Long appointmentId,
        Instant meetAt,
        Integer reminderMinutes
) {
    public static AppointmentResponse from(AppointmentDto dto) {
        return new AppointmentResponse(dto.appointmentId(), dto.meetAt(), dto.reminderMinutes());
    }
}
