package com.goods.market.trade.application;

import com.goods.market.chat.domain.ChatRoom;
import com.goods.market.chat.domain.ChatRoomStatus;
import com.goods.market.chat.infrastructure.ChatRoomRepository;
import com.goods.market.common.event.DomainEventPublisher;
import com.goods.market.common.event.events.TradeAppointmentReminderDueEvent;
import com.goods.market.common.event.events.TradeAppointmentCanceledEvent;
import com.goods.market.common.event.events.TradeAppointmentScheduledEvent;
import com.goods.market.common.event.events.TradeAppointmentTradePromptEvent;
import com.goods.market.trade.application.dto.AppointmentDto;
import com.goods.market.trade.application.dto.TradePromptDto;
import com.goods.market.trade.domain.Appointment;
import com.goods.market.trade.domain.AppointmentStatus;
import com.goods.market.trade.infrastructure.AppointmentRepository;
import com.goods.market.listing.domain.Listing;
import com.goods.market.listing.domain.Status;
import com.goods.market.listing.infrastructure.ListingJpaRepository;
import com.goods.market.member.domain.Member;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
import com.goods.market.trade.exception.AppointmentBadRequestException;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentCommandServiceImpl implements AppointmentCommandService {

    private final AppointmentRepository appointmentRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ListingJpaRepository listingJpaRepository;
    private final MemberJpaRepository memberJpaRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional
    public AppointmentDto schedule(Long memberId, Long chatRoomId, Instant meetAt, Integer reminderMinutes) {
        ChatRoom chatRoom = findParticipatingChatRoom(memberId, chatRoomId);
        Listing listing = listingJpaRepository.findByIdAndDeletedAtIsNull(chatRoom.getListingId())
                .orElseThrow(EntityNotFoundException::new);

        validateSchedulableListing(listing, chatRoom);

        appointmentRepository.findTopByListingIdAndBuyerIdAndStatusOrderByCreatedAtDesc(
                        chatRoom.getListingId(),
                        chatRoom.getBuyerId(),
                        AppointmentStatus.SCHEDULED
                )
                .ifPresent(Appointment::cancel); // 기존 약속이 존재한다면 cancel

        // 새 약속 생성
        Appointment appointment = Appointment.schedule(
                chatRoom.getListingId(),
                chatRoom.getSellerId(),
                chatRoom.getBuyerId(),
                meetAt,
                reminderMinutes
        );
        Appointment saved = appointmentRepository.save(appointment);

        domainEventPublisher.publish(new TradeAppointmentScheduledEvent(
                saved.getId(),
                saved.getListingId(),
                saved.getSellerId(),
                saved.getBuyerId(),
                saved.getNotificationTime()
        ));

        return new AppointmentDto(saved.getId(), saved.getMeetAt(), saved.getReminderMinutes());
    }

    @Override
    @Transactional
    public void cancel(Long memberId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findByIdAndStatus(appointmentId, AppointmentStatus.SCHEDULED)
                .orElseThrow(EntityNotFoundException::new);
        validateParticipant(appointment, memberId);
        appointment.cancel();

        domainEventPublisher.publish(new TradeAppointmentCanceledEvent(
                appointment.getId(),
                appointment.getListingId(),
                appointment.getSellerId(),
                appointment.getBuyerId()
        ));
    }

    @Override
    public Optional<TradePromptDto> getTradePrompt(Long memberId) {
        return appointmentRepository
                .findBySellerIdAndStatusAndTradePromptSentAtIsNotNullAndTradePromptDismissedAtIsNullOrderByTradePromptSentAtDesc(
                        memberId,
                        AppointmentStatus.SCHEDULED
                )
                .stream()
                .filter(this::isTradePromptEligible)
                .findFirst()
                .map(this::toTradePromptResponse);
    }

    /**
     * 거래 완료를 미루기
     * @param memberId
     * @param appointmentId
     */
    @Override
    @Transactional
    public void dismissTradePrompt(Long memberId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findByIdAndStatus(appointmentId, AppointmentStatus.SCHEDULED)
                .orElseThrow(EntityNotFoundException::new);

        if (!appointment.getSellerId().equals(memberId)) {
            throw new EntityNotFoundException();
        }
        appointment.dismissTradePrompt();
    }

    /**
     * 약속 시간 전 알림과, 약속 시간 후 거래완료 여부 확인 이벤트 발행
     */
    @Override
    @Transactional
    public void processDueNotifications() {
        Instant now = Instant.now();

        for (Appointment appointment : appointmentRepository
                .findByStatusAndNotificationTimeLessThanEqualAndReminderSentAtIsNull(AppointmentStatus.SCHEDULED, now)) {
            appointment.markReminderSent(); // 리마인더를 보냈다고 마킹

            domainEventPublisher.publish(new TradeAppointmentReminderDueEvent(
                    appointment.getId(),
                    appointment.getListingId(),
                    appointment.getSellerId(),
                    appointment.getBuyerId()
            ));
        }

        // 약속 시간 후 거래 완료했는지 알리는 팝업 이벤트
        Instant promptThreshold = now.minus(Duration.ofMinutes(3)); // 약속 시간 후 3분이 지났는지 확인

        for (Appointment appointment : appointmentRepository
                .findByStatusAndTradePromptSentAtIsNullAndMeetAtLessThanEqual(AppointmentStatus.SCHEDULED, promptThreshold)) {
            if (!isTradePromptEligible(appointment)) {
                continue;
            }

            // 거래 여부 알림 마킹 (보냈다 표시)
            appointment.markTradePromptSent();
            domainEventPublisher.publish(new TradeAppointmentTradePromptEvent(
                    appointment.getId(),
                    appointment.getListingId(),
                    appointment.getSellerId(),
                    appointment.getBuyerId()
            ));
        }
    }

    private ChatRoom findParticipatingChatRoom(Long memberId, Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .filter(room -> room.getStatus() == ChatRoomStatus.ACTIVE)
                .filter(room -> room.isParticipant(memberId))
                .orElseThrow(EntityNotFoundException::new);
    }

    private void validateParticipant(Appointment appointment, Long memberId) {
        if (!appointment.getSellerId().equals(memberId) && !appointment.getBuyerId().equals(memberId)) {
            throw new EntityNotFoundException();
        }
    }

    private void validateSchedulableListing(Listing listing, ChatRoom chatRoom) {
        if (listing.getStatus() == Status.SOLD_OUT) {
            throw new AppointmentBadRequestException("거래 완료된 게시글입니다.");
        }
        if (listing.getStatus() == Status.RESERVED
                && !Objects.equals(listing.getReserverId(), chatRoom.getBuyerId())) {
            throw new AppointmentBadRequestException("다른 사람과 거래중입니다.");
        }
        if (listing.getStatus() != Status.PUBLISHED && listing.getStatus() != Status.RESERVED) {
            throw new AppointmentBadRequestException("약속을 만들 수 없는 게시글입니다.");
        }
    }

    // 판매글이 삭제되지 않고, 예약중인지 확인
    private boolean isTradePromptEligible(Appointment appointment) {
        Listing listing = listingJpaRepository.findByIdAndDeletedAtIsNull(appointment.getListingId())
                .orElse(null);

        if (listing == null || listing.getStatus() != Status.RESERVED) {
            return false;
        }

        return chatRoomRepository.findByListingIdAndBuyerIdAndStatus(
                        appointment.getListingId(),
                        appointment.getBuyerId(),
                        ChatRoomStatus.ACTIVE
                )
                .isPresent();
    }

    private TradePromptDto toTradePromptResponse(Appointment appointment) {
        ChatRoom chatRoom = chatRoomRepository.findByListingIdAndBuyerIdAndStatus(
                        appointment.getListingId(),
                        appointment.getBuyerId(),
                        ChatRoomStatus.ACTIVE
                )
                .orElseThrow(EntityNotFoundException::new);
        Listing listing = listingJpaRepository.findByIdAndDeletedAtIsNull(appointment.getListingId())
                .orElseThrow(EntityNotFoundException::new);

        Member buyer = memberJpaRepository.findById(appointment.getBuyerId())
                .orElse(null);

        return new TradePromptDto(
                appointment.getId(),
                appointment.getListingId(),
                chatRoom.getId(),
                appointment.getBuyerId(),
                buyer != null ? buyer.getNickname() : "",
                listing.getTitle()
        );
    }
}
