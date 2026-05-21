package com.goods.market.member.infrastructure.memberRegion.mapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;


@Mapper
public interface MemberRegionMapper {

    boolean isPointInRegion(
            @Param("regionId") Integer regionId,
            @Param("lat") BigDecimal lat,
            @Param("lng") BigDecimal lng
    );
}
