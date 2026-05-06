package com.goods.market.notification.infrastructure;

import com.goods.market.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    void deleteByMemberId(Long memberId);
}
