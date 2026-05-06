package com.goods.market.common.domain.geojson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FeatureCollection {
    private String type;
    private String name;
    private Crs crs;
    private List<Feature> features;
}
