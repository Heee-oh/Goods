package com.goods.market.notification.application.dto;

import java.time.Instant;

public record AppointmentReminderPayload(
        String notification_type,
        Long appointment_id,
        Long chat_room_id,
        String partner_nickname,
        Instant meet_at,
        Integer reminder_minutes
) {
}
