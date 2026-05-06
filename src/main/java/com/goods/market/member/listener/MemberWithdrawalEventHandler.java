package com.goods.market.member.listener;

import com.goods.market.common.event.events.MemberWithdrawnEvent;
import com.goods.market.listing.infrastructure.ListingJpaRepository;
import com.goods.market.member.infrastructure.memberRegion.MemberRegionJpaRepository;
import com.goods.market.notification.infrastructure.KeywordSubscriptionRepository;
import com.goods.market.notification.infrastructure.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberWithdrawalEventHandler {

    private final NotificationRepository notificationRepository;
    private final KeywordSubscriptionRepository keywordSubscriptionRepository;
    private final MemberRegionJpaRepository memberRegionJpaRepository;
    private final ListingJpaRepository listingJpaRepository;

    @EventListener
    public void handle(MemberWithdrawnEvent event) {
        notificationRepository.deleteByMemberId(event.memberId());
        keywordSubscriptionRepository.deleteByMemberId(event.memberId());
        memberRegionJpaRepository.deleteByMember_Id(event.memberId());
        listingJpaRepository.hideAllBySellerId(event.memberId());
    }
}
