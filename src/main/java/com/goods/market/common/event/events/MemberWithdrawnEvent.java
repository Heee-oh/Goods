package com.goods.market.common.event.events;

import com.goods.market.common.event.DomainEvent;

public record MemberWithdrawnEvent(Long memberId) implements DomainEvent {

    @Override
    public String aggregateType() {
        return "MEMBER";
    }

    @Override
    public Long aggregateId() {
        return memberId;
    }
}
