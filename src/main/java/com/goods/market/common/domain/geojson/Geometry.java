package com.goods.market.common.domain.geojson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Geometry {
    private String type;
    private List<List<List<List<Double>>>> coordinates;
}
