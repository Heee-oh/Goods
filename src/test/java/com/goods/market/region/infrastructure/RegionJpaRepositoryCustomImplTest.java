package com.goods.market.region.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class RegionJpaRepositoryCustomImplTest {

    @Autowired
    RegionJpaRepository regionJpaRepository;

    @Test
    @DisplayName("서울시청 좌표가 regionId 20에 포함되면 true를 반환한다")
    void validateCoordinateInRegionReturnsTrueWhenPointIsCoveredByRegion() {
        BigDecimal lat = new BigDecimal("37.5665");
        BigDecimal lng = new BigDecimal("126.9780");
        Integer regionId = 20;

        boolean result = regionJpaRepository.validateCoordinateInRegion(regionId, lat, lng);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("멀리 떨어진 좌표가 regionId 20에 포함되지 않으면 false를 반환한다")
    void validateCoordinateInRegionReturnsFalseWhenPointIsOutsideRegion() {
        BigDecimal lat = new BigDecimal("35.1796");
        BigDecimal lng = new BigDecimal("129.0756");
        Integer regionId = 20;

        boolean result = regionJpaRepository.validateCoordinateInRegion(regionId, lat, lng);

        assertThat(result).isFalse();
    }
}