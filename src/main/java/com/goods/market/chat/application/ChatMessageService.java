package com.goods.market.chat.application;

import com.goods.market.chat.application.dto.ChatMessageSendResult;
import com.goods.market.chat.application.dto.ChatMessageCreateDto;
import com.goods.market.chat.application.dto.ChatMessageDto;
import com.goods.market.common.event.DomainEventPublisher;
import com.goods.market.common.event.events.ChatMessageSentEvent;
import com.goods.market.chat.domain.ChatMessage;
import com.goods.market.chat.domain.ChatRead;
import com.goods.market.chat.domain.ChatRoom;
import com.goods.market.chat.domain.ChatRoomStatus;
import com.goods.market.chat.exception.ChatReadNotFoundException;
import com.goods.market.chat.exception.ChatRoomInactiveException;
import com.goods.market.chat.exception.ChatRoomNotFoundException;
import com.goods.market.chat.exception.ChatRoomParticipantException;
import com.goods.market.chat.infrastructure.ChatMessageRepository;
import com.goods.market.chat.infrastructure.ChatReadRepository;
import com.goods.market.chat.infrastructure.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatReadRepository chatReadRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public ChatMessageSendResult saveMessage(ChatMessageCreateDto request, Long senderId) {
        ChatRoom chatRoom = chatRoomRepository.findById(request.chatRoomId())
                .orElseThrow(() -> new ChatRoomNotFoundException("채팅방이 없습니다."));

        if (!chatRoom.isParticipant(senderId)) {
            log.info("parti == {} {}", chatRoom.getBuyerId(), chatRoom.getSellerId());
            log.info("sender = {}", senderId);
            throw new ChatRoomParticipantException("채팅방 참여자가 아닙니다.");
        }

        if (chatRoom.getStatus() != ChatRoomStatus.ACTIVE) {
            throw new ChatRoomInactiveException("비활성화된 채팅방입니다.");
        }

        boolean firstMessage = !chatMessageRepository.existsByChatRoomId(chatRoom.getId());

        ChatMessage message = ChatMessage.create(senderId, request.chatRoomId(), request.type(), request.content());
        ChatMessage saved = chatMessageRepository.save(message);

        ChatRead senderRead = chatReadRepository.findByChatRoomIdAndMemberId(chatRoom.getId(), senderId)
                .orElseThrow(() -> new ChatReadNotFoundException("읽음 정보가 없습니다."));

        senderRead.markAsRead(saved.getId());

        Long otherMemberId = chatRoom.getBuyerId().equals(senderId)
                ? chatRoom.getSellerId()
                : chatRoom.getBuyerId();

        domainEventPublisher.publish(new ChatMessageSentEvent(
                saved.getId(),
                chatRoom.getId(),
                senderId,
                otherMemberId,
                saved.getType(),
                saved.getContent()
        ));

        ChatMessageDto response = new ChatMessageDto(
                chatRoom.getId(),
                saved.getId(),
                saved.getSenderId(),
                saved.getType(),
                saved.getContent(),
                saved.getCreatedAt()
        );

        return new ChatMessageSendResult(response, otherMemberId, firstMessage);
    }
}
