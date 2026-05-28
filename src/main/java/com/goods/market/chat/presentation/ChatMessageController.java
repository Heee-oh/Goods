package com.goods.market.chat.presentation;

import com.goods.market.chat.application.ChatMessageService;
import com.goods.market.chat.application.dto.ChatMessageSendResult;
import com.goods.market.chat.presentation.dto.ChatMessageRequest;
import com.goods.market.chat.presentation.dto.response.ChatMessageResponse;
import com.goods.market.common.auth.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatMessageController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageService chatMessageService;

    /**
     * 클라이언트가 메시지를 발행(Publish)할 때 호출되는 메서드
     * 클라이언트 설정: stompClient.send("/pub/chat/message", {}, JSON_데이터)
     */
    @MessageMapping("/chat/message")
    public void sendMessage(@Payload ChatMessageRequest request,
                            Principal principal) { // payload : @RequestBody 처럼 JSON -> DTO 객체로 변환

        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        AuthPrincipal authPrincipal = (AuthPrincipal) auth.getPrincipal();

        // 1. 비즈니스 로직: 전달받은 메시지를 DB에 저장하고, 응답용 DTO로 변환
        ChatMessageSendResult result = chatMessageService.saveMessage(request.toDto(), authPrincipal.memberId());
        ChatMessageResponse response = ChatMessageResponse.from(result.message());

        // 2. 브로드캐스팅: 해당 채팅방을 구독(Subscribe)하고 있는 모든 클라이언트에게 메시지 전송
        // 목적지 URL: /sub/chat/room/{chatRoomId}
        messagingTemplate.convertAndSend(
                "/sub/chat/room/" + response.chatRoomId(),
                response
        );

        if (result.firstMessage()) {
            messagingTemplate.convertAndSend(
                    "/sub/members/" + result.receiverId() + "/chat-events",
                    response
            );
        }
    }
}
