package com.goods.market.common.domain.geojson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor

public class Feature {
    private String type;
    private Geometry geometry;
    private FeatureProperties properties;
}
