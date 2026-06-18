package com.goods.market.chat.application;

import com.goods.market.chat.application.dto.ChatMessageCreateDto;
import com.goods.market.chat.domain.ChatRoom;
import com.goods.market.chat.domain.MessageType;
import com.goods.market.chat.exception.ChatRoomInactiveException;
import com.goods.market.chat.infrastructure.ChatMessageRepository;
import com.goods.market.chat.infrastructure.ChatReadRepository;
import com.goods.market.chat.infrastructure.ChatRoomRepository;
import com.goods.market.listing.domain.Listing;
import com.goods.market.listing.domain.TransactionType;
import com.goods.market.listing.domain.ListingRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatReadRepository chatReadRepository;

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Test
    void saveMessageRejectsSoldOutListing() {
        ChatRoom chatRoom = ChatRoom.create(10L, 1L, 2L);
        Listing listing = createSoldOutListing();

        when(chatRoomRepository.findById(20L)).thenReturn(Optional.of(chatRoom));
        when(listingRepository.findActiveById(10L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> chatMessageService.saveMessage(
                new ChatMessageCreateDto(20L, MessageType.TEXT, "안녕하세요"),
                2L
        ))
                .isInstanceOf(ChatRoomInactiveException.class)
                .hasMessageContaining("거래 완료된 게시글은 채팅할 수 없습니다.");

        verify(chatMessageRepository, never()).save(any());
        verify(chatReadRepository, never()).findByChatRoomIdAndMemberId(anyLong(), anyLong());
    }

    private Listing createSoldOutListing() {
        Listing listing = Listing.createDraft(
                1L,
                "title",
                "description",
                1L,
                1000L,
                TransactionType.SELL,
                null,
                List.of()
        );
        listing.publish();
        listing.reserve(2L);
        listing.markSoldOut(2L);
        return listing;
    }
}
