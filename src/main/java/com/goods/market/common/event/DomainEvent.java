package com.goods.market.common.event;

public interface DomainEvent {

    String aggregateType();

    Long aggregateId();

    default String eventType() {
        return getClass().getSimpleName();
    }
}
