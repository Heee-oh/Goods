package com.goods.market.chat.presentation;

import com.goods.market.common.api.ApiResponse;
import com.goods.market.chat.application.ChatQueryService;
import com.goods.market.chat.application.ChatRoomService;
import com.goods.market.chat.presentation.dto.ChatRoomCreateRequest;
import com.goods.market.chat.presentation.dto.ChatRoomCreateResponse;
import com.goods.market.chat.presentation.dto.ChatRoomDetailResponse;
import com.goods.market.chat.presentation.dto.ChatRoomSummaryResponse;
import com.goods.market.common.auth.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/chat-rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatQueryService chatQueryService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomCreateResponse>> getOrCreateChatRoom(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request,
            @RequestBody ChatRoomCreateRequest chatRoomCreateRequest) {
        Long chatRoomId = chatRoomService.getOrCreateChatRoom(
                chatRoomCreateRequest.listingId(),
                principal.memberId()
        );

        return ResponseEntity.ok(ApiResponse.success(new ChatRoomCreateResponse(chatRoomId), request.getRequestURI()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomSummaryResponse>>> getChatRooms(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                chatQueryService.getChatRooms(principal.memberId()),
                request.getRequestURI()
        ));
    }


    @GetMapping("/{chat_room_id}")
    public ResponseEntity<ApiResponse<ChatRoomDetailResponse>> getChatRoom(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request,
            @PathVariable("chat_room_id") Long chatRoomId) {

        return ResponseEntity.ok(ApiResponse.success(
                chatQueryService.getChatRoom(principal.memberId(), chatRoomId),
                request.getRequestURI()
        ));
    }
}
