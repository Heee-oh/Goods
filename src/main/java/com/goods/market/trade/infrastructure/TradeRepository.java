package com.goods.market.trade.infrastructure;

import com.goods.market.trade.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    boolean existsByListingId(Long listingId);

    Optional<Trade> findByListingId(Long listingId);
}
