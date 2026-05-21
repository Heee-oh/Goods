package com.goods.market.trade.presentation;

import com.goods.market.common.auth.AuthPrincipal;
import com.goods.market.common.api.ApiResponse;
import com.goods.market.trade.application.AppointmentCommandService;
import com.goods.market.trade.application.dto.AppointmentDto;
import com.goods.market.trade.application.dto.TradePromptDto;
import com.goods.market.trade.presentation.dto.response.AppointmentResponse;
import com.goods.market.trade.presentation.dto.response.TradePromptResponse;
import com.goods.market.trade.presentation.dto.request.AppointmentCreateRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AppointmentController {

    private final AppointmentCommandService appointmentCommandService;

    @PostMapping("/chat-rooms/{chat_room_id}/appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> schedule(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("chat_room_id") Long chatRoomId,
            HttpServletRequest httpRequest,
            @Valid @RequestBody AppointmentCreateRequest appointmentCreateRequest
    ) {
        AppointmentDto appointment = appointmentCommandService.schedule(
                principal.memberId(),
                chatRoomId,
                appointmentCreateRequest.meetAt(),
                appointmentCreateRequest.reminderMinutes()
        );
        return ResponseEntity.ok(ApiResponse.success(
                AppointmentResponse.from(appointment),
                httpRequest.getRequestURI()
        ));
    }

    @DeleteMapping("/appointments/{appointment_id}")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("appointment_id") Long appointmentId
    ) {
        appointmentCommandService.cancel(principal.memberId(), appointmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/appointments/trade-prompt")
    public ResponseEntity<?> getTradePrompt(@AuthenticationPrincipal AuthPrincipal principal) {
        Optional<TradePromptDto> prompt = appointmentCommandService.getTradePrompt(principal.memberId());
        return prompt.<ResponseEntity<?>>map(dto -> ResponseEntity.ok(TradePromptResponse.from(dto)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/appointments/{appointment_id}/trade-prompt/dismiss")
    public ResponseEntity<Void> dismissTradePrompt(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("appointment_id") Long appointmentId
    ) {
        appointmentCommandService.dismissTradePrompt(principal.memberId(), appointmentId);
        return ResponseEntity.noContent().build();
    }
}
