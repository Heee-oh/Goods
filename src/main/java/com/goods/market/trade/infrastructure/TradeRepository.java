package com.goods.market.trade.infrastructure;

import com.goods.market.trade.domain.Trade;
import com.goods.market.trade.domain.TradeStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long>, TradeQueryRepositoryCustom {

    Optional<Trade> findByListingId(Long listingId);

    List<Trade> findByBuyerIdAndStatusOrderByCompletedAtDesc(Long buyerId, TradeStatus status);

}
