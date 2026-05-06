package com.goods.market.trade.application;

import com.goods.market.chat.domain.ChatRoom;
import com.goods.market.chat.domain.ChatRoomStatus;
import com.goods.market.chat.infrastructure.ChatRoomRepository;
import com.goods.market.common.event.DomainEventPublisher;
import com.goods.market.common.event.events.TradeAppointmentReminderDueEvent;
import com.goods.market.common.event.events.TradeAppointmentScheduledEvent;
import com.goods.market.common.event.events.TradeAppointmentTradePromptEvent;
import com.goods.market.trade.application.dto.AppointmentResponse;
import com.goods.market.trade.application.dto.TradePromptResponse;
import com.goods.market.trade.domain.Appointment;
import com.goods.market.trade.domain.AppointmentStatus;
import com.goods.market.trade.infrastructure.AppointmentRepository;
import com.goods.market.listing.domain.Listing;
import com.goods.market.listing.domain.Status;
import com.goods.market.listing.infrastructure.ListingJpaRepository;
import com.goods.market.member.domain.Member;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
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
public class AppointmentCommandServiceImpl implements AppointmentCommandService {

    private final AppointmentRepository appointmentRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ListingJpaRepository listingJpaRepository;
    private final MemberJpaRepository memberJpaRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional
    public AppointmentResponse schedule(Long memberId, Long chatRoomId, Instant meetAt, Integer reminderMinutes) {
        ChatRoom chatRoom = findParticipatingChatRoom(memberId, chatRoomId);

        appointmentRepository.findTopByListingIdAndBuyerIdAndStatusOrderByCreatedAtDesc(
                        chatRoom.getListingId(),
                        chatRoom.getBuyerId(),
                        AppointmentStatus.SCHEDULED
                )
                .ifPresent(Appointment::cancel);

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

        return new AppointmentResponse(saved.getId(), saved.getMeetAt(), saved.getReminderMinutes());
    }

    @Override
    @Transactional
    public void cancel(Long memberId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findByIdAndStatus(appointmentId, AppointmentStatus.SCHEDULED)
                .orElseThrow(EntityNotFoundException::new);
        validateParticipant(appointment, memberId);
        appointment.cancel();
    }

    @Override
    public Optional<TradePromptResponse> getTradePrompt(Long memberId) {
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

    @Override
    @Transactional
    public void processDueNotifications() {
        Instant now = Instant.now();

        for (Appointment appointment : appointmentRepository
                .findByStatusAndNotificationTimeLessThanEqualAndReminderSentAtIsNull(AppointmentStatus.SCHEDULED, now)) {
            appointment.markReminderSent();
            domainEventPublisher.publish(new TradeAppointmentReminderDueEvent(
                    appointment.getId(),
                    appointment.getListingId(),
                    appointment.getSellerId(),
                    appointment.getBuyerId()
            ));
        }

        Instant promptThreshold = now.minus(Duration.ofMinutes(3));
        for (Appointment appointment : appointmentRepository
                .findByStatusAndTradePromptSentAtIsNullAndMeetAtLessThanEqual(AppointmentStatus.SCHEDULED, promptThreshold)) {
            if (!isTradePromptEligible(appointment)) {
                continue;
            }

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

    private TradePromptResponse toTradePromptResponse(Appointment appointment) {
        ChatRoom chatRoom = chatRoomRepository.findByListingIdAndBuyerIdAndStatus(
                        appointment.getListingId(),
                        appointment.getBuyerId(),
                        ChatRoomStatus.ACTIVE
                )
                .orElseThrow(EntityNotFoundException::new);

        Member buyer = memberJpaRepository.findById(appointment.getBuyerId())
                .orElse(null);

        return new TradePromptResponse(
                appointment.getId(),
                appointment.getListingId(),
                chatRoom.getId(),
                appointment.getBuyerId(),
                buyer != null ? buyer.getNickname() : ""
        );
    }
}
