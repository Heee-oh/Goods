package com.goods.market.common.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;

public class GeometryUtil {

    private static final GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);

    public static Point createPoint(BigDecimal lng, BigDecimal lat) {
        return factory.createPoint(new Coordinate(lng.doubleValue(), lat.doubleValue()));
    }
}
