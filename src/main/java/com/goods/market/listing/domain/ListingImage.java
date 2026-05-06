package com.goods.market.listing.domain;

import com.goods.market.listing.exception.ListingBadRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(name = "listing_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ListingImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", updatable = false, nullable = false)
    private Listing listing;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void initCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    protected ListingImage(int sortOrder, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new ListingBadRequestException("Invalid imageUrl");
        }
        this.sortOrder = sortOrder;
        this.imageUrl = imageUrl;
    }

    public static ListingImage of(String imageUrl) {
        return new ListingImage(0, imageUrl);
    }

    public void updateSortOrder(int sortOrder) {
        if (sortOrder < 0) {
            throw new ListingBadRequestException("sortOrder cannot be negative");
        }
        this.sortOrder = sortOrder;
    }

    public void updateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new ListingBadRequestException("Invalid imageUrl");
        }

        this.imageUrl = imageUrl;
    }

    public void updateListing(Listing listing) {
        this.listing = listing;
    }
}
