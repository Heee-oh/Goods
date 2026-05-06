package com.goods.market.trade.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(name = "trade")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "price", nullable = false)
    private Long price;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TradeStatus status;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Trade complete(Long listingId, Long sellerId, Long buyerId, Long price) {
        Trade trade = new Trade();
        trade.listingId = listingId;
        trade.sellerId = sellerId;
        trade.buyerId = buyerId;
        trade.price = price;
        trade.status = TradeStatus.COMPLETED;
        trade.completedAt = Instant.now();
        trade.createdAt = Instant.now();
        trade.updatedAt = trade.createdAt;
        return trade;
    }
}
