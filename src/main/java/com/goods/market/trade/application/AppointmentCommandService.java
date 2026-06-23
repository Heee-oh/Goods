package com.goods.market.trade.application;

import com.goods.market.chat.application.ChatQueryService;
import com.goods.market.chat.application.dto.AppointmentChatRoomDto;
import com.goods.market.common.event.DomainEventPublisher;
import com.goods.market.common.event.events.TradeAppointmentReminderDueEvent;
import com.goods.market.common.event.events.TradeAppointmentCanceledEvent;
import com.goods.market.common.event.events.TradeAppointmentScheduledEvent;
import com.goods.market.common.event.events.TradeAppointmentTradePromptEvent;
import com.goods.market.listing.application.ListingQueryService;
import com.goods.market.listing.application.dto.AppointmentListingDto;
import com.goods.market.member.application.MemberQueryService;
import com.goods.market.member.application.dto.AppointmentMemberDto;
import com.goods.market.trade.application.dto.AppointmentDto;
import com.goods.market.trade.application.dto.TradePromptDto;
import com.goods.market.trade.domain.Appointment;
import com.goods.market.trade.domain.AppointmentSchedulingPolicy;
import com.goods.market.trade.domain.AppointmentStatus;
import com.goods.market.trade.infrastructure.AppointmentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentCommandService {

    private final AppointmentRepository appointmentRepository;
    private final ChatQueryService chatQueryService;
    private final ListingQueryService listingQueryService;
    private final MemberQueryService memberQueryService;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public AppointmentDto schedule(Long memberId, Long chatRoomId, Instant meetAt, Integer reminderMinutes) {
        AppointmentChatRoomDto chatRoom = chatQueryService.getParticipatingAppointmentChatRoom(memberId, chatRoomId);
        AppointmentListingDto listing = listingQueryService.getAppointmentListing(chatRoom.listingId());

        AppointmentSchedulingPolicy.validateSchedulableListing(
                listing.status(),
                listing.reserverId(),
                chatRoom.buyerId()
        );

        appointmentRepository.findTopByListingIdAndBuyerIdAndStatusOrderByCreatedAtDesc(
                        chatRoom.listingId(),
                        chatRoom.buyerId(),
                        AppointmentStatus.SCHEDULED
                )
                .ifPresent(Appointment::cancel); // 기존 약속이 존재한다면 cancel

        // 새 약속 생성
        Appointment appointment = Appointment.schedule(
                chatRoom.listingId(),
                chatRoom.sellerId(),
                chatRoom.buyerId(),
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

    private void validateParticipant(Appointment appointment, Long memberId) {
        if (!appointment.getSellerId().equals(memberId) && !appointment.getBuyerId().equals(memberId)) {
            throw new EntityNotFoundException();
        }
    }

    // 판매글이 삭제되지 않고, 예약중인지 확인
    private boolean isTradePromptEligible(Appointment appointment) {
        AppointmentListingDto listing = listingQueryService.findAppointmentListing(appointment.getListingId())
                .orElse(null);

        if (listing == null || !AppointmentSchedulingPolicy.isReservedListing(listing.status())) {
            return false;
        }

        return chatQueryService.findActiveAppointmentChatRoom(appointment.getListingId(), appointment.getBuyerId())
                .isPresent();
    }

    private TradePromptDto toTradePromptResponse(Appointment appointment) {
        AppointmentChatRoomDto chatRoom = chatQueryService.findActiveAppointmentChatRoom(
                        appointment.getListingId(),
                        appointment.getBuyerId()
                )
                .orElseThrow(EntityNotFoundException::new);
        AppointmentListingDto listing = listingQueryService.getAppointmentListing(appointment.getListingId());

        String buyerNickname = memberQueryService.findAppointmentMember(appointment.getBuyerId())
                .map(AppointmentMemberDto::nickname)
                .orElse("");

        return new TradePromptDto(
                appointment.getId(),
                appointment.getListingId(),
                chatRoom.chatRoomId(),
                appointment.getBuyerId(),
                buyerNickname,
                listing.title()
        );
    }
}
