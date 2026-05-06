package com.goods.market.common.event;

import com.goods.market.common.event.outbox.OutboxEvent;
import com.goods.market.common.event.outbox.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * 도메인 이벤트를 아웃박스에 저장한 뒤 애플리케이션 내부로 발행한다.
     */
    public void publish(DomainEvent event) {
        outboxEventRepository.save(toOutboxEvent(event));
        applicationEventPublisher.publishEvent(event);
    }

    private OutboxEvent toOutboxEvent(DomainEvent event) {
        try {
            return OutboxEvent.create(
                    event.aggregateType(),
                    event.aggregateId(),
                    event.eventType(),
                    objectMapper.writeValueAsString(event)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize domain event", e);
        }
    }
}
