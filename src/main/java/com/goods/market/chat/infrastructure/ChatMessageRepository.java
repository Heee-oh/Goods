package com.goods.market.chat.infrastructure;

import com.goods.market.chat.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAscIdAsc(Long chatRoomId);

    Optional<ChatMessage> findTopByChatRoomIdOrderByCreatedAtDescIdDesc(Long chatRoomId);

    boolean existsByChatRoomId(Long chatRoomId);
}
