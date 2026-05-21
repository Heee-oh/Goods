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

    // 거래 완료 여부 알림을 보냈고, 거래 완료를 미루지 않은 약속들을 거래완료 여부 알림 보낸 시간 최신순 기준으로 조회
    List<Appointment> findBySellerIdAndStatusAndTradePromptSentAtIsNotNullAndTradePromptDismissedAtIsNullOrderByTradePromptSentAtDesc(
            Long sellerId,
            AppointmentStatus status
    );
}
