package com.goods.market.trade.application;

import com.goods.market.common.event.DomainEventPublisher;
import com.goods.market.common.event.events.TradeCompletedEvent;
import com.goods.market.trade.domain.Price;
import com.goods.market.trade.domain.Trade;
import com.goods.market.trade.domain.TransactionType;
import com.goods.market.trade.infrastructure.TradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeCommandServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private TradeCommandService tradeCommandService;

    @Test
    void completeCreatesTradeAndPublishesExchangeEvent() {
        Price price = new Price(3000L, TransactionType.SELL);
        when(tradeRepository.findByListingId(10L)).thenReturn(Optional.empty());
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> {
            Trade trade = invocation.getArgument(0);
            ReflectionTestUtils.setField(trade, "id", 99L);
            return trade;
        });

        Long tradeId = tradeCommandService.complete(10L, 1L, 2L, price);

        assertThat(tradeId).isEqualTo(99L);
        ArgumentCaptor<TradeCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TradeCompletedEvent.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().tradeId()).isEqualTo(99L);
        assertThat(eventCaptor.getValue().listingId()).isEqualTo(10L);
        assertThat(eventCaptor.getValue().price().getPriceAmount()).isEqualTo(3000L);
        assertThat(eventCaptor.getValue().price().resolveTransactionType()).isEqualTo(TransactionType.SELL);
    }

    @Test
    void completeReturnsExistingTradeWithoutPublishingDuplicateEvent() {
        Price price = new Price(3000L, TransactionType.SELL);
        Trade trade = Trade.complete(10L, 1L, 2L, price);
        ReflectionTestUtils.setField(trade, "id", 99L);
        when(tradeRepository.findByListingId(10L)).thenReturn(Optional.of(trade));

        Long tradeId = tradeCommandService.complete(10L, 1L, 2L, price);

        assertThat(tradeId).isEqualTo(99L);
        verify(tradeRepository, never()).save(any());
        verify(domainEventPublisher, never()).publish(any());
    }
}
