package com.goods.market.trade.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Entity
@Getter
@Table(name = "appointment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id")
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AppointmentStatus status;

    @Column(name = "notification_time")
    private Instant notificationTime;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    @Column(name = "meet_at", nullable = false)
    private Instant meetAt;

    @Column(name = "meet_place_text", length = 200)
    private String meetPlaceText;

    @Column(name = "meet_lat", precision = 10, scale = 7)
    private BigDecimal meetLat;

    @Column(name = "meet_lng", precision = 10, scale = 7)
    private BigDecimal meetLng;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "trade_prompt_sent_at")
    private Instant tradePromptSentAt;

    @Column(name = "trade_prompt_dismissed_at")
    private Instant tradePromptDismissedAt;

    public static Appointment schedule(
            Long listingId,
            Long sellerId,
            Long buyerId,
            Instant meetAt,
            Integer reminderMinutes
    ) {
        Appointment appointment = new Appointment();
        appointment.listingId = listingId;
        appointment.sellerId = sellerId;
        appointment.buyerId = buyerId;
        appointment.status = AppointmentStatus.SCHEDULED;
        appointment.meetAt = meetAt;
        appointment.notificationTime = reminderMinutes != null
                ? meetAt.minus(Duration.ofMinutes(reminderMinutes))
                : null;
        appointment.createdAt = Instant.now();
        appointment.updatedAt = appointment.createdAt;
        return appointment;
    }

    public void cancel() {
        status = AppointmentStatus.CANCELED;
        updatedAt = Instant.now();
    }

    public void markReminderSent() {
        reminderSentAt = Instant.now();
        updatedAt = reminderSentAt;
    }

    public void markTradePromptSent() {
        tradePromptSentAt = Instant.now();
        updatedAt = tradePromptSentAt;
    }

    public void dismissTradePrompt() {
        tradePromptDismissedAt = Instant.now();
        updatedAt = tradePromptDismissedAt;
    }

    public void markDone() {
        status = AppointmentStatus.DONE;
        updatedAt = Instant.now();
    }

    public Integer getReminderMinutes() {
        if (notificationTime == null) {
            return null;
        }

        long minutes = Duration.between(notificationTime, meetAt).toMinutes();
        return Math.toIntExact(minutes);
    }
}
