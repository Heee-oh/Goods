package com.goods.market.listing.domain;

import java.util.Optional;

public interface ListingRepository {

    Listing save(Listing listing);

    Optional<Listing> findActiveById(Long listingId);

    Optional<Listing> findActiveByIdForEdit(Long listingId);
}
