package com.goods.market.chat.application;

import com.goods.market.chat.domain.ChatRoom;
import com.goods.market.chat.domain.ChatRoomStatus;
import com.goods.market.chat.exception.ChatRoomNotFoundException;
import com.goods.market.chat.infrastructure.ChatMessageRepository;
import com.goods.market.chat.infrastructure.ChatRoomRepository;
import com.goods.market.chat.presentation.dto.ChatMessageItemResponse;
import com.goods.market.chat.presentation.dto.ChatRoomAppointmentResponse;
import com.goods.market.chat.presentation.dto.ChatRoomDetailResponse;
import com.goods.market.chat.presentation.dto.ChatRoomSummaryResponse;
import com.goods.market.trade.domain.AppointmentStatus;
import com.goods.market.trade.infrastructure.AppointmentRepository;
import com.goods.market.listing.domain.Listing;
import com.goods.market.listing.domain.ListingImage;
import com.goods.market.listing.infrastructure.ListingJpaRepository;
import com.goods.market.member.domain.Member;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
import java.util.List;
import java.util.Comparator;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatQueryServiceImpl implements ChatQueryService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ListingJpaRepository listingJpaRepository;
    private final MemberJpaRepository memberJpaRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    public List<ChatRoomSummaryResponse> getChatRooms(Long memberId) {
        return chatRoomRepository.findSummariesByMemberId(memberId);
    }

    @Override
    public ChatRoomDetailResponse getChatRoom(Long memberId, Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .filter(room -> room.getStatus() == ChatRoomStatus.ACTIVE)
                .filter(room -> room.isParticipant(memberId))
                .orElseThrow(() -> new ChatRoomNotFoundException("채팅방을 찾을 수 없습니다."));

        Listing listing = listingJpaRepository.findActiveByIdWithImages(chatRoom.getListingId())
                .orElse(null);
        String listingImageUrl = listing != null
                ? listing.getImages().stream()
                .sorted(Comparator.comparingInt(ListingImage::getSortOrder))
                .map(ListingImage::getImageUrl)
                .findFirst()
                .orElse(null)
                : null;

        Long partnerId = getPartnerId(chatRoom, memberId);
        Member partner = memberJpaRepository.findById(partnerId)
                .orElse(null);

        List<ChatMessageItemResponse> messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtAscIdAsc(chatRoomId)
                .stream()
                .map(message -> new ChatMessageItemResponse(
                        message.getId(),
                        message.getSenderId(),
                        message.getType(),
                        message.getContent(),
                        message.getCreatedAt()
                ))
                .toList();

        ChatRoomAppointmentResponse currentAppointment = appointmentRepository
                .findTopByListingIdAndBuyerIdAndStatusOrderByCreatedAtDesc(
                        chatRoom.getListingId(),
                        chatRoom.getBuyerId(),
                        AppointmentStatus.SCHEDULED
                )
                .map(appointment -> new ChatRoomAppointmentResponse(
                        appointment.getId(),
                        appointment.getMeetAt(),
                        appointment.getReminderMinutes()
                ))
                .orElse(null);

        return new ChatRoomDetailResponse(
                chatRoom.getId(),
                chatRoom.getListingId(),
                partnerId,
                partner != null ? partner.getNickname() : "상대 사용자",
                partner != null ? partner.getProfileImageUrl() : null,
                partner != null ? partner.getSmileScore() : null,
                listing != null ? listing.getTitle() : "상품 정보를 불러올 수 없어요.",
                listing != null && listing.getPrice() != null ? listing.getPrice().getPriceAmount() : null,
                currentAppointment,
                messages
        );
    }

    private Long getPartnerId(ChatRoom chatRoom, Long memberId) {
        if (Objects.equals(chatRoom.getSellerId(), memberId)) {
            return chatRoom.getBuyerId();
        }

        return chatRoom.getSellerId();
    }
}
