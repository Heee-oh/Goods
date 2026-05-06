package com.goods.market.listing.domain;

import com.goods.market.listing.exception.ListingBadRequestException;
import com.goods.market.listing.exception.ListingConflictException;
import com.goods.market.common.domain.BaseTimeEntity;
import com.github.f4b6a3.tsid.TsidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
@Getter
@Table(name = "listing")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Listing extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "listing_id")
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "buyer_id")
    private Long buyerId;

    @Column(name = "reserver_id")
    private Long reserverId;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "region_id")
    private Integer regionId;

    @Column(name = "origin_region_id")
    private Integer originRegionId;

    @Column(name = "origin_lat", precision = 10, scale = 7)
    private java.math.BigDecimal originLat;

    @Column(name = "origin_lng", precision = 10, scale = 7)
    private java.math.BigDecimal originLng;

    @Embedded
    private HopeLocation hopeLocation;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Status status;

    @Embedded
    private Price price;

    @Column(name = "is_hidden")
    private boolean isHidden;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    List<ListingImage> images = new ArrayList<>();

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "deleted_at")
    private Instant deletedAt;



    public static Listing draft() {
        Listing l = new Listing();
        l.status = Status.DRAFT;
        l.isHidden = false;
        return l;
    }

    public static Listing createEmptyDraft(Long sellerId, Integer regionId) {
        return createEmptyDraft(sellerId, regionId, null, null, null);
    }

    public static Listing createEmptyDraft(
            Long sellerId,
            Integer regionId,
            Integer originRegionId,
            java.math.BigDecimal originLat,
            java.math.BigDecimal originLng
    ) {
        Listing listing = draft();
        listing.regionId = regionId;
        listing.originRegionId = originRegionId;
        listing.originLat = originLat;
        listing.originLng = originLng;

        if (sellerId == null || sellerId <= 0) {
            throw new ListingBadRequestException("Invalid seller id");
        }

        listing.sellerId = sellerId;
        listing.title = "Draft";
        listing.price = new Price(0L, TransactionType.SELL);
        return listing;
    }

    public static Listing createDraft(
            Long sellerId,
            String title,
            String description,
            Long categoryId,
            Long priceAmount,
            TransactionType transactionType,
            HopeLocation hopeLocation,
            List<ListingImage> listingImages
    ) {
        Listing listing = draft();

        if (sellerId == null || sellerId <= 0) {
            throw new ListingBadRequestException("Invalid seller id");
        }
        if (title == null || title.isBlank() || title.length() > 200) {
            throw new ListingBadRequestException("Title must be 1 to 200 characters");
        }

        listing.sellerId = sellerId;
        listing.title = title;
        listing.description = description;
        listing.categoryId = categoryId;
        listing.updatePrice(priceAmount, transactionType);
        listing.hopeLocation = hopeLocation;

        if (listingImages != null && !listingImages.isEmpty()) {
            listing.addImages(listingImages);
        }

        return listing;
    }

    @Deprecated
    public static Listing createDraft(
            Long sellerId,
            String title,
            String description,
            Long categoryId,
            Long priceAmount,
            boolean isFree,
            HopeLocation hopeLocation,
            List<ListingImage> listingImages
    ) {
        return createDraft(
                sellerId,
                title,
                description,
                categoryId,
                priceAmount,
                isFree ? TransactionType.FREE : TransactionType.SELL,
                hopeLocation,
                listingImages
        );
    }

    public static Listing draftPrice(Long amount, TransactionType transactionType) {
        Listing l = new Listing();
        l.status = Status.DRAFT;
        l.isHidden = false;
        l.updatePrice(amount, transactionType);

        return l;
    }

    @Deprecated
    public static Listing draftPrice(Long amount, boolean isFree) {
        return draftPrice(amount, isFree ? TransactionType.FREE : TransactionType.SELL);
    }

    public void publish() {
        if (isDeleted()) {
            throw new ListingConflictException("Deleted listing");
        }
        if (status != Status.DRAFT) {
            throw new ListingConflictException("Publish failed");
        }

        status = Status.PUBLISHED;
    }

    public void hide() {
        if (isDeleted()) {
            throw new ListingConflictException("Deleted listing");
        }
        if (status == Status.DRAFT) {
            throw new ListingConflictException("Draft cannot be hidden");
        }

        isHidden = true;
    }

    public void unHide() {
        if (isDeleted()) {
            throw new ListingConflictException("Deleted listing");
        }
        if (!isHidden) {
            throw new ListingConflictException("Already visible");
        }
        if (status == Status.DRAFT) {
            throw new ListingConflictException("Draft state");
        }

        isHidden = false;
    }

    public void reserve(Long reservedId) {
        if (reservedId == null) {
            throw new ListingBadRequestException("Invalid buyer id");
        }
        if (isDeleted()) {
            throw new ListingConflictException("Deleted listing");
        }
        if (isHidden) {
            throw new ListingConflictException("Hidden state");
        }
        if (status != Status.PUBLISHED) {
            throw new ListingConflictException("Cannot reserve in current status");
        }

        this.reserverId = reservedId;
        status = Status.RESERVED;
    }

    public void cancelReserve() {
        if (isDeleted()) {
            throw new ListingConflictException("Deleted listing");
        }
        if (status != Status.RESERVED) {
            throw new ListingConflictException("Not reserved state");
        }

        status = Status.PUBLISHED;
        this.reserverId = null;
    }

    public void markSoldOut(Long buyerId) {
        if (isDeleted()) {
            throw new ListingConflictException("Deleted listing");
        }
        if (status != Status.RESERVED) {
            throw new ListingConflictException("Not reserved state");
        }
        if (!this.reserverId.equals(buyerId)) {
            throw new ListingBadRequestException("Invalid buyer id");
        }

        status = Status.SOLD_OUT;
        this.buyerId = buyerId;
    }

    public void remove() {
        if (isDeleted()) {
            throw new ListingConflictException("Already deleted");
        }
        if (status != Status.DRAFT && status != Status.PUBLISHED) {
            throw new ListingConflictException("Delete precondition failed");
        }

        deletedAt = Instant.now();
    }

    public void updatePrice(Long nPrice, TransactionType transactionType) {
        ensureEditable();

        if (transactionType == null) {
            throw new ListingBadRequestException("Invalid transaction type");
        }

        if (transactionType.isSell()) {
            if (nPrice == null || nPrice <= 0) {
                throw new ListingBadRequestException("Invalid price");
            }
        } else if (nPrice != null && nPrice != 0) {
            throw new ListingBadRequestException("Invalid price");
        }

        this.price = new Price(transactionType.isSell() ? nPrice : 0L, transactionType);
    }

    @Deprecated
    public void updatePrice(Long nPrice, boolean isFree) {
        updatePrice(nPrice, isFree ? TransactionType.FREE : TransactionType.SELL);
    }

    public void updateTitleAndDescription(String title, String description) {
        ensureEditable();

        if (title == null || title.isBlank() || title.length() > 200) {
            throw new ListingBadRequestException("Title must be 1 to 200 characters");
        }
        this.title = title;

        if (description == null) {
            throw new ListingBadRequestException("Description is required");
        }
        this.description = description;
    }

    public void updateHopeLocation(HopeLocation newHopeLocation) {
        ensureEditable();
        hopeLocation = newHopeLocation;
    }

    public void updateCategory(Long categoryId) {
        ensureEditable();

        if (categoryId == null || categoryId <= 0) {
            throw new ListingBadRequestException("Invalid category id");
        }
        this.categoryId = categoryId;
    }

    public void addImages(List<ListingImage> listingImages) {
        ensureEditable();
        listingImages.forEach(this::addImage);
    }

    public void deleteImages(List<Long> imageIds) {
        ensureEditable();

        HashSet<Long> idSet = new HashSet<>(imageIds);

        boolean removed = images.removeIf(image -> {
            if (idSet.contains(image.getImageId())) {
                image.updateListing(null);
                return true;
            }
            return false;
        });

        if (!removed) {
            throw new ListingBadRequestException("Image not found");
        }

        reorderImages();
    }

    public void replaceImages(List<ListingImage> listingImages) {
        ensureEditable();

        for (ListingImage img : images) {
            img.updateListing(null);
        }
        images.clear();

        for (ListingImage image : listingImages) {
            addImage(image);
        }
    }

    public void updateImageOrder(List<Long> orderedImageIds) {
        ensureEditable();

        if (orderedImageIds.size() != images.size()) {
            throw new ListingBadRequestException("Image count mismatch");
        }

        Map<Long, ListingImage> map = images.stream()
                .collect(Collectors.toMap(ListingImage::getImageId, Function.identity()));

        for (int i = 0; i < orderedImageIds.size(); i++) {
            ListingImage img = map.get(orderedImageIds.get(i));
            if (img == null) {
                throw new ListingBadRequestException("Image not found");
            }
            img.updateSortOrder(i);
        }
    }

    private void addImage(ListingImage image) {
        images.add(image);
        image.updateListing(this);
        image.updateSortOrder(images.size() - 1);
    }

    private void reorderImages() {
        for (int i = 0; i < images.size(); i++) {
            images.get(i).updateSortOrder(i);
        }
    }

    private void ensureEditable() {
        if (isDeleted()) {
            throw new ListingConflictException("Deleted listing");
        }
        if (status != Status.DRAFT && status != Status.PUBLISHED) {
            throw new ListingConflictException("Cannot edit in current state");
        }
    }

    private boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isOwnedBy(Long memberId) {
        return Objects.equals(sellerId, memberId);
    }
}
