package com.goods.market.chat.infrastructure;

import com.goods.market.chat.domain.ChatRoomStatus;
import com.goods.market.chat.domain.QChatMessage;
import com.goods.market.chat.domain.QChatRoom;
import com.goods.market.chat.presentation.dto.ChatRoomSummaryResponse;
import com.goods.market.listing.domain.QListing;
import com.goods.market.listing.domain.QListingImage;
import com.goods.market.member.domain.QMember;
import com.goods.market.region.domain.QRegion;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatRoomRepositoryCustomImpl implements ChatRoomRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QChatRoom chatRoom = QChatRoom.chatRoom;
    private final QListing listing = QListing.listing;
    private final QListingImage image = QListingImage.listingImage;
    private final QRegion region = QRegion.region;
    private final QMember seller = new QMember("seller");
    private final QMember buyer = new QMember("buyer");
    private final QChatMessage lastMessage = new QChatMessage("lastMessage");
    private final QChatMessage lastMessageSub = new QChatMessage("lastMessageSub");

    @Override
    public List<ChatRoomSummaryResponse> findSummariesByMemberId(Long memberId) {
        StringExpression resolvedTransactionType = Expressions.stringTemplate(
                "coalesce(lower({0}), case when {1} = 0 then 'free' else 'sell' end)",
                listing.price.transactionType,
                listing.price.priceAmount
        );

        return queryFactory
                .select(Projections.constructor(
                        ChatRoomSummaryResponse.class,
                        chatRoom.id,
                        listing.id,
                        listing.title,
                        listing.price.priceAmount,
                        listing.status.stringValue(),
                        resolvedTransactionType,
                        image.imageUrl,
                        chatRoom.sellerId.eq(memberId),
                        new CaseBuilder()
                                .when(chatRoom.sellerId.eq(memberId)).then(buyer.nickname)
                                .otherwise(seller.nickname),
                        new CaseBuilder()
                                .when(chatRoom.sellerId.eq(memberId)).then(buyer.profileImageUrl)
                                .otherwise(seller.profileImageUrl),
                        region.dongnm,
                        lastMessage.content.coalesce(""),
                        lastMessage.createdAt.coalesce(chatRoom.createdAt)
                ))
                .from(chatRoom)
                .leftJoin(listing).on(chatRoom.listingId.eq(listing.id))
                .leftJoin(image).on(
                        listing.id.eq(image.listing.id)
                                .and(image.sortOrder.isNull().or(image.sortOrder.eq(0)))
                )
                .leftJoin(region).on(listing.regionId.eq(region.id))
                .leftJoin(seller).on(chatRoom.sellerId.eq(seller.id))
                .leftJoin(buyer).on(chatRoom.buyerId.eq(buyer.id))
                .leftJoin(lastMessage).on(
                        lastMessage.id.eq(
                                JPAExpressions
                                        .select(lastMessageSub.id.max())
                                        .from(lastMessageSub)
                                        .where(lastMessageSub.chatRoomId.eq(chatRoom.id))
                        )
                )
                .where(
                        chatRoom.status.eq(ChatRoomStatus.ACTIVE),
                        chatRoom.sellerId.eq(memberId).or(chatRoom.buyerId.eq(memberId))
                )
                .orderBy(lastMessage.createdAt.coalesce(chatRoom.createdAt).desc())
                .fetch();
    }
}
