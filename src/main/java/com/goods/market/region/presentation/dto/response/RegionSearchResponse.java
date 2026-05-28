package com.goods.market.region.presentation.dto.response;

import com.goods.market.region.domain.Region;

public record RegionSearchResponse(
        Integer regionId,
        String fullName,
        String dongnm
) {
    public static RegionSearchResponse from(Region region) {
        return new RegionSearchResponse(
                region.getId(),
                region.getAdmNm(),
                region.getDongnm()
        );
    }
}
