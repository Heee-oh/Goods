package com.goods.market.trade.presentation.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record AppointmentCreateRequest(
        @NotNull
        @Future
        Instant meetAt,

        @Min(1)
        @Max(1440)
        Integer reminderMinutes
) {
}
