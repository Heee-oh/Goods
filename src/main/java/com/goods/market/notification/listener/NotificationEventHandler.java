package com.goods.market.notification.listener;

import com.goods.market.chat.domain.ChatRoom;
import com.goods.market.chat.domain.ChatRoomStatus;
import com.goods.market.chat.infrastructure.ChatRoomRepository;
import com.goods.market.common.event.events.TradeAppointmentReminderDueEvent;
import com.goods.market.notification.application.dto.AppointmentReminderPayload;
import com.goods.market.notification.application.dto.ReviewRequestPayload;
import com.goods.market.common.event.events.ChatMessageSentEvent;
import com.goods.market.common.event.events.ChatStartedEvent;
import com.goods.market.common.event.events.TradeAppointmentCanceledEvent;
import com.goods.market.common.event.events.TradeAppointmentTradePromptEvent;
import com.goods.market.common.event.events.TradeCompletedEvent;
import com.goods.market.common.event.events.ListingCreatedEvent;
import com.goods.market.common.event.events.ListingReservationCanceledEvent;
import com.goods.market.common.event.events.ListingSoldOutEvent;
import com.goods.market.listing.domain.ListingRepository;
import com.goods.market.member.domain.Member;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
import com.goods.market.notification.domain.KeywordSubscription;
import com.goods.market.notification.domain.Notification;
import com.goods.market.notification.domain.NotificationType;
import com.goods.market.notification.infrastructure.KeywordSubscriptionRepository;
import com.goods.market.notification.infrastructure.NotificationRepository;
import com.goods.market.trade.domain.Appointment;
import com.goods.market.trade.infrastructure.AppointmentRepository;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final NotificationRepository notificationRepository;
    private final KeywordSubscriptionRepository keywordSubscriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberJpaRepository memberJpaRepository;
    private final ListingRepository listingRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handle(TradeAppointmentCanceledEvent event) {
        if (event.buyerId() == null) {
            return;
        }
        notificationRepository.save(Notification.create(event.buyerId(), NotificationType.RESERVATION_CANCELED));
    }

    @EventListener
    public void handle(ListingReservationCanceledEvent event) {
        if (event.reserverId() == null) {
            return;
        }
        notificationRepository.save(Notification.create(event.reserverId(), NotificationType.RESERVATION_CANCELED));
    }

    @EventListener
    public void handle(ListingSoldOutEvent event) {
        notificationRepository.save(Notification.create(event.sellerId(), NotificationType.SOLD_OUT));
        notificationRepository.save(Notification.create(event.buyerId(), NotificationType.SOLD_OUT));
    }

    @EventListener
    public void handle(TradeCompletedEvent event) {
        notificationRepository.save(Notification.create(event.buyerId(), NotificationType.REVIEW_REQUEST));
        sendReviewRequest(event);
    }

    @EventListener
    public void handle(ChatStartedEvent event) {
        notificationRepository.save(Notification.create(event.sellerId(), NotificationType.NEW_CHAT));
    }

    @EventListener
    public void handle(ChatMessageSentEvent event) {
        notificationRepository.save(Notification.create(event.otherMemberId(), NotificationType.NEW_CHAT));
    }

    @EventListener
    public void handle(TradeAppointmentReminderDueEvent event) {
        notificationRepository.save(Notification.create(event.sellerId(), NotificationType.APPOINTMENT_ALARM));
        notificationRepository.save(Notification.create(event.buyerId(), NotificationType.APPOINTMENT_ALARM));

        appointmentRepository.findById(event.appointmentId())
                .ifPresent(appointment -> {
                    sendAppointmentReminder(event.sellerId(), event.buyerId(), appointment);
                    sendAppointmentReminder(event.buyerId(), event.sellerId(), appointment);
                });
    }

    @EventListener
    public void handle(TradeAppointmentTradePromptEvent event) {
        notificationRepository.save(Notification.create(event.sellerId(), NotificationType.TRADE_COMPLETE_PROMPT));
    }

    @EventListener
    public void handle(ListingCreatedEvent event) {
        if (event.title() == null || event.title().isBlank()) {
            return;
        }

        String normalizedTitle = event.title().toLowerCase(Locale.ROOT);
        Set<Long> notifiedMemberIds = new HashSet<>();
        for (KeywordSubscription subscription : keywordSubscriptionRepository.findByRegionIdOrRegionIdIsNull(event.regionId())) {
            if (normalizedTitle.contains(subscription.getKeyword().toLowerCase(Locale.ROOT))
                    && notifiedMemberIds.add(subscription.getMemberId())) {
                notificationRepository.save(Notification.create(subscription.getMemberId(), NotificationType.LISTING_KEYWORD));
            }
        }
    }

    private void sendAppointmentReminder(Long memberId, Long partnerId, Appointment appointment) {
        ChatRoom chatRoom = chatRoomRepository.findByListingIdAndBuyerIdAndStatus(
                        appointment.getListingId(),
                        appointment.getBuyerId(),
                        ChatRoomStatus.ACTIVE
                )
                .orElse(null);
        Member partner = memberJpaRepository.findById(partnerId).orElse(null);

        messagingTemplate.convertAndSend(
                "/sub/members/" + memberId + "/notifications",
                new AppointmentReminderPayload(
                        "APPOINTMENT_ALARM",
                        appointment.getId(),
                        chatRoom != null ? chatRoom.getId() : null,
                        partner != null ? partner.getNickname() : "상대방",
                        appointment.getMeetAt(),
                        appointment.getReminderMinutes()
                )
        );
    }

    private void sendReviewRequest(TradeCompletedEvent event) {
        String listingTitle = listingRepository.findActiveById(event.listingId())
                .map(listing -> listing.getTitle() == null ? "" : listing.getTitle())
                .orElse("");
        String sellerNickname = memberJpaRepository.findById(event.sellerId())
                .map(Member::getNickname)
                .orElse("상대방");

        messagingTemplate.convertAndSend(
                "/sub/members/" + event.buyerId() + "/notifications",
                new ReviewRequestPayload(
                        "REVIEW_REQUEST",
                        event.tradeId(),
                        listingTitle,
                        sellerNickname,
                        false
                )
        );
    }
}
