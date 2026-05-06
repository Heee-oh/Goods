package com.goods.market.chat.domain;

import com.goods.market.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(name = "chat_room")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ChatRoomStatus status;

    public static ChatRoom create(Long listingId, Long sellerId, Long buyerId) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.listingId = listingId;
        chatRoom.sellerId = sellerId;
        chatRoom.buyerId = buyerId;
        chatRoom.status = ChatRoomStatus.ACTIVE;

        return chatRoom;
    }

    public boolean isParticipant(Long memberId) {
        return buyerId.equals(memberId) || sellerId.equals(memberId);
    }

    public void syncSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

}
