package com.goods.market.chat.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(name = "chat_message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;


    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static ChatMessage create(Long senderId, Long chatRoomId, MessageType type, String content) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.chatRoomId = chatRoomId;
        chatMessage.senderId = senderId;
        chatMessage.type = type;
        chatMessage.content = content;
        chatMessage.createdAt = Instant.now();

        return chatMessage;
    }
}

/**
 * 채팅방 id과 보낸이, 그리고 메시지 타입, 내용을 저장
 * 받는이를 따로 저장하지 않는 이유는 채팅방 id를 따로 기록하고있고, 멀티챗이 가능하게끔하기 위해
 */