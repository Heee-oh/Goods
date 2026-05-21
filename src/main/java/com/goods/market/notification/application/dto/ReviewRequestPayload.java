package com.goods.market.notification.application.dto;

public record ReviewRequestPayload(
        String notification_type,
        Long trade_id,
        String listing_title,
        String partner_nickname,
        boolean writer_is_seller
) {
}
