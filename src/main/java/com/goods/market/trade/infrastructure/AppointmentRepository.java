package com.goods.market.trade.infrastructure;

import com.goods.market.trade.domain.Appointment;
import com.goods.market.trade.domain.AppointmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findTopByListingIdAndBuyerIdAndStatusOrderByCreatedAtDesc(
            Long listingId,
            Long buyerId,
            AppointmentStatus status
    );

    Optional<Appointment> findByIdAndStatus(Long appointmentId, AppointmentStatus status);

    List<Appointment> findByStatusAndNotificationTimeLessThanEqualAndReminderSentAtIsNull(
            AppointmentStatus status,
            Instant threshold
    );

    List<Appointment> findByStatusAndTradePromptSentAtIsNullAndMeetAtLessThanEqual(
            AppointmentStatus status,
            Instant threshold
    );

    List<Appointment> findBySellerIdAndStatusAndTradePromptSentAtIsNotNullAndTradePromptDismissedAtIsNullOrderByTradePromptSentAtDesc(
            Long sellerId,
            AppointmentStatus status
    );
}
