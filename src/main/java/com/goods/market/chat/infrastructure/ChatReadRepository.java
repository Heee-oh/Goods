package com.goods.market.chat.infrastructure;

import com.goods.market.chat.domain.ChatRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatReadRepository extends JpaRepository<ChatRead, Long> {

    // 특정 채팅방에서 특정 유저의 '읽음 정보'를 가져올 때 사용
    Optional<ChatRead> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);
}
