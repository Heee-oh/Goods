package com.goods.market.listing.domain;

import com.goods.market.listing.exception.ListingBadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HopeLocation {

    private static final BigDecimal MIN_LAT = new BigDecimal("-90");
    private static final BigDecimal MAX_LAT = new BigDecimal("90");
    private static final BigDecimal MIN_LNG = new BigDecimal("-180");
    private static final BigDecimal MAX_LNG = new BigDecimal("180");

    @Column(name = "hope_region_id")
    private Integer regionId;

    @Column(name = "hope_lat", precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(name = "hope_lng", precision = 10, scale = 7)
    private BigDecimal lng;

    public HopeLocation(Integer regionId, BigDecimal lat, BigDecimal lng) {
        if (regionId == null) {
            throw new ListingBadRequestException("regionId is required");
        }
        if (lat == null || lat.compareTo(MIN_LAT) < 0 || lat.compareTo(MAX_LAT) > 0) {
            throw new ListingBadRequestException("latitude out of range");
        }
        if (lng == null || lng.compareTo(MIN_LNG) < 0 || lng.compareTo(MAX_LNG) > 0) {
            throw new ListingBadRequestException("longitude out of range");
        }

        this.regionId = regionId;
        this.lat = lat;
        this.lng = lng;
    }
}
