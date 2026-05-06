package com.goods.market.chat.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(name = "chat_read")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_read_id")
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static ChatRead create(Long chatRoomId, Long memberId) {
        ChatRead chatRead = new ChatRead();
        chatRead.chatRoomId = chatRoomId;
        chatRead.memberId = memberId;

        // 아무 메시지도 읽지 않았으므로 초기값을 0으로 세팅
        chatRead.lastReadMessageId = 0L;
        chatRead.updatedAt = Instant.now();

        return chatRead;
    }

    public void markAsRead(Long messageId) {
        if (this.lastReadMessageId == null || this.lastReadMessageId < messageId) {
            this.lastReadMessageId = messageId;
            this.updatedAt = Instant.now();
        }
    }
}
