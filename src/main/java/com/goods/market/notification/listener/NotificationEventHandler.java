package com.goods.market.notification.listener;

import com.goods.market.common.event.events.ChatMessageSentEvent;
import com.goods.market.common.event.events.ChatStartedEvent;
import com.goods.market.common.event.events.TradeAppointmentReminderDueEvent;
import com.goods.market.common.event.events.TradeAppointmentTradePromptEvent;
import com.goods.market.common.event.events.TradeCompletedEvent;
import com.goods.market.common.event.events.ListingCreatedEvent;
import com.goods.market.common.event.events.ListingReservationCanceledEvent;
import com.goods.market.common.event.events.ListingSoldOutEvent;
import com.goods.market.notification.domain.KeywordSubscription;
import com.goods.market.notification.domain.Notification;
import com.goods.market.notification.domain.NotificationType;
import com.goods.market.notification.infrastructure.KeywordSubscriptionRepository;
import com.goods.market.notification.infrastructure.NotificationRepository;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final NotificationRepository notificationRepository;
    private final KeywordSubscriptionRepository keywordSubscriptionRepository;

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
}
