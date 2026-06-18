package com.goods.market.listing.infrastructure;

import com.goods.market.listing.domain.Listing;
import com.goods.market.listing.domain.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaListingRepositoryAdapter implements ListingRepository {

    private final ListingJpaRepository listingJpaRepository;

    @Override
    public Listing save(Listing listing) {
        return listingJpaRepository.save(listing);
    }

    @Override
    public Optional<Listing> findActiveById(Long listingId) {
        return listingJpaRepository.findByIdAndDeletedAtIsNull(listingId);
    }

    @Override
    public Optional<Listing> findActiveByIdForEdit(Long listingId) {
        return listingJpaRepository.findActiveByIdWithImages(listingId);
    }
}
