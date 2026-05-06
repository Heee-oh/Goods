package com.goods.market.common.domain.geojson;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FeatureProperties {
    @JsonProperty("adm_nm")
    private String admNm;

    @JsonProperty("adm_cd2")
    private String admCd2;

    @JsonProperty("sgg")
    private String sgg;

    @JsonProperty("sido")
    private String sido;

    @JsonProperty("sidonm")
    private String sidonm;

    @JsonProperty("sggnm")
    private String sggnm;

    @JsonProperty("adm_cd")
    private String admCd;
}
