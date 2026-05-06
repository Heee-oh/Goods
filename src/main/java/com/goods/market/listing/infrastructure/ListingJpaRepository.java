package com.goods.market.listing.infrastructure;

import com.goods.market.listing.domain.Listing;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface ListingJpaRepository extends JpaRepository<Listing, Long>, ListingJpaRepositoryCustom {

    @Transactional(readOnly = true)
    Optional<Listing> findByIdAndDeletedAtIsNull(Long listingId);

    @Transactional(readOnly = true)
    @EntityGraph(attributePaths = "images")
    @Query("SELECT l FROM Listing l WHERE l.id = :listingId AND l.deletedAt IS NULL")
    Optional<Listing> findActiveByIdWithImages(@Param("listingId") Long listingId);

    @Transactional(readOnly = true)
    @EntityGraph(attributePaths = "images")
    @Query("SELECT l FROM Listing l WHERE l.id IN :listingIds AND l.deletedAt IS NULL")
    List<Listing> findActiveByIdInWithImages(@Param("listingIds") Collection<Long> listingIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Listing l
               SET l.isHidden = true
             WHERE l.sellerId = :sellerId
               AND l.deletedAt IS NULL
            """)
    int hideAllBySellerId(@Param("sellerId") Long sellerId);

}

