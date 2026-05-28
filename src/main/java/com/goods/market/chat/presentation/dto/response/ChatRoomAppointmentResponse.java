package com.goods.market.chat.presentation.dto.response;

import com.goods.market.chat.application.dto.ChatRoomAppointmentDto;

import java.time.Instant;

public record ChatRoomAppointmentResponse(
        Long appointmentId,
        Instant meetAt,
        Integer reminderMinutes
) {
    public static ChatRoomAppointmentResponse from(ChatRoomAppointmentDto dto) {
        return new ChatRoomAppointmentResponse(dto.appointmentId(), dto.meetAt(), dto.reminderMinutes());
    }
}
