package com.goods.market.region.infrastructure;

import com.goods.market.region.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Repository
@Transactional
public interface RegionJpaRepository extends JpaRepository<Region, Integer>, RegionJpaRepositoryCustom {

    @Query(value =
            """
                SELECT 1
                FROM region
                WHERE ST_DWithin(
                    geom,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    20 -- 오차 범위
                )
                AND id = :regionId
                LIMIT 1;
            """
            , nativeQuery = true)
    Integer covers(Integer regionId, BigDecimal lat, BigDecimal lng);

    default boolean validateCoordinateInRegion(Integer regionId, BigDecimal lat, BigDecimal lng) {
        return covers(regionId, lat, lng) != null;
    }

    @Query(
            value = """
                SELECT *
                FROM region r
                WHERE :keyword = ''
                   OR r.adm_nm LIKE CONCAT('%', :keyword, '%')
                   OR r.dongnm LIKE CONCAT('%', :keyword, '%')
                ORDER BY r.sidonm, r.sggnm, r.dongnm
                LIMIT 20
            """,
            nativeQuery = true
    )
    List<Region> searchByKeyword(String keyword);

    @Query(
            value = """
                SELECT *
                FROM region r
                ORDER BY ST_Distance(
                    r.geom::geometry,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geometry
                ) ASC
                LIMIT 20
            """,
            nativeQuery = true
    )
    List<Region> findNearby(BigDecimal lat, BigDecimal lng);
}
