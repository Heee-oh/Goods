package com.goods.market.trade.infrastructure;

import com.goods.market.trade.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TradeJpaRepository extends JpaRepository<Trade, Long>, TradeQueryRepositoryCustom {

    Optional<Trade> findByListingId(Long listingId);


}
