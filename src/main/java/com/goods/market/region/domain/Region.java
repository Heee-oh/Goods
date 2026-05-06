package com.goods.market.region.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.MultiPolygon;

@Entity
@Getter
@Table(name = "region")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "adm_nm", length = 50, nullable = false)
    private String admNm;

    @Column(name = "adm_cd", length = 8, nullable = false)
    private String admCd;

    @Column(name = "adm_cd2", length = 10, nullable = false)
    private String admCd2;

    @Column(length = 5, nullable = false)
    private String sgg;

    @Column(length = 2, nullable = false)
    private String sido;

    @Column(length = 20, nullable = false)
    private String sidonm;

    @Column(length = 20, nullable = false)
    private String sggnm;

    @Column(length = 20, nullable = false)
    private String dongnm;

    @Column(columnDefinition = "geography(MultiPolygon, 4326)", nullable = false)
    private MultiPolygon geom;
}
