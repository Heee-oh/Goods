package com.goods.market.member.infrastructure.Interest;

import com.goods.market.member.domain.Interest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface InterestJpaRepository extends JpaRepository<Interest, Long>, InterestRepositoryCustom {

    @Modifying
    @Query("DELETE FROM Interest i WHERE i.listingId = :listingId AND i.member.id = :memberId")
    int deleteByListingIdAndMemberId(
            @Param("listingId") Long listingId,
            @Param("memberId") Long memberId
    );

    @Transactional(readOnly = true)
    @Query("SELECT COUNT(i) > 0 FROM Interest i WHERE i.listingId = :listingId AND i.member.id = :memberId")
    boolean existsByListingIdAndMemberId(
            @Param("listingId") Long listingId,
            @Param("memberId") Long memberId
    );
}
