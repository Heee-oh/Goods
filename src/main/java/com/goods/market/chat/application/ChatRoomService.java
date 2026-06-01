package com.goods.market.chat.application;

import com.goods.market.chat.exception.ChatRoomParticipantException;
import com.goods.market.listing.infrastructure.ListingJpaRepository;
import com.goods.market.common.event.DomainEventPublisher;
import com.goods.market.common.event.events.ChatStartedEvent;
import com.goods.market.chat.domain.ChatRead;
import com.goods.market.chat.domain.ChatRoom;
import com.goods.market.chat.domain.ChatRoomStatus;
import com.goods.market.chat.exception.ChatRoomInactiveException;
import com.goods.market.chat.exception.ChatRoomNotFoundException;
import com.goods.market.listing.domain.Listing;
import com.goods.market.listing.domain.Status;
import com.goods.market.chat.infrastructure.ChatReadRepository;
import com.goods.market.chat.infrastructure.ChatRoomRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatReadRepository chatReadRepository;
    private final ListingJpaRepository listingJpaRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public Long getOrCreateChatRoom(Long listingId, Long buyerId) {
        Listing listing = listingJpaRepository.findByIdAndDeletedAtIsNull(listingId)
                .orElseThrow(() -> new ChatRoomNotFoundException("게시글을 찾을 수 없습니다."));

        if (listing.getStatus() == Status.SOLD_OUT) {
            throw new ChatRoomInactiveException("거래 완료된 게시글은 채팅할 수 없습니다.");
        }


        Long sellerId = listing.getSellerId();
        if (sellerId.equals(buyerId)) {
            throw new ChatRoomParticipantException("자신의 판매글에는 채팅할 수 없습니다.");
        }

        Optional<ChatRoom> existingRoom = chatRoomRepository
                .findByListingIdAndBuyerIdAndStatus(listingId, buyerId, ChatRoomStatus.ACTIVE);

        if (existingRoom.isPresent()) {
            ChatRoom room = existingRoom.get();
            if (!room.getSellerId().equals(sellerId)) {
                room.syncSellerId(sellerId);
            }
            return room.getId();
        }

        ChatRoom chatRoom = ChatRoom.create(listingId, sellerId, buyerId);
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        ChatRead buyerRead = ChatRead.create(savedRoom.getId(), buyerId);
        ChatRead sellerRead = ChatRead.create(savedRoom.getId(), sellerId);

        chatReadRepository.save(buyerRead);
        chatReadRepository.save(sellerRead);

        // 채팅방 생성 이벤트를 발행한다.
        domainEventPublisher.publish(new ChatStartedEvent(
                savedRoom.getId(),
                listingId,
                buyerId,
                sellerId
        ));

        return savedRoom.getId();
    }
}
