package com.goods.market.domain.geojson;

import com.goods.market.common.domain.geojson.Feature;
import com.goods.market.common.domain.geojson.FeatureProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Profile("init")
@SpringBootTest
public class GeoJsonInitPis {
    @Autowired
    private NamedParameterJdbcTemplate template;
//
//    @Test
//    void geo_data() throws IOException {
//        ClassPathResource resource =
//                new ClassPathResource("geo/HangJeongDong_ver20250401.geojson");
//
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode root = mapper.readTree(resource.getInputStream());
//
//        JsonNode featuresNode = root.get("features");
//        Feature[] features = mapper.readValue(featuresNode.toString(), Feature[].class);
//
//        String sql = """
//            INSERT INTO region
//              (adm_nm, adm_cd, adm_cd2, sgg, sido, sidonm, sggnm, dongnm, geom)
//            VALUES
//              (:adm_nm, :adm_cd, :adm_cd2, :sgg, :sido, :sidonm, :sggnm, :dongnm,
//               ST_SetSRID(ST_GeomFromGeoJSON(:geom_json), 4326))
//            """;
//
//        List<SqlParameterSource> batchParams = new ArrayList<>(features.length);
//
//        for (Feature feature : features) {
//            FeatureProperties properties = feature.getProperties();
//
//            String[] split = properties.getAdmNm().split(" ");
//            String dongnm = split[split.length - 1];
//
//            // geometry는 "그대로" JSON 문자열로 넘김 (WKT 조립 제거)
//            String geomJson = mapper.writeValueAsString(feature.getGeometry());
//
//            MapSqlParameterSource param = new MapSqlParameterSource();
//            param.addValue("adm_nm", properties.getAdmNm());
//            param.addValue("adm_cd", properties.getAdmCd());
//            param.addValue("adm_cd2", properties.getAdmCd2());
//            param.addValue("sgg", properties.getSgg());
//            param.addValue("sido", properties.getSido());
//            param.addValue("sidonm", properties.getSidonm());
//            param.addValue("sggnm", properties.getSggnm());
//            param.addValue("dongnm", dongnm);
//            param.addValue("geom_json", geomJson);
//
//            batchParams.add(param);
//        }
//        System.out.println(mapper.writeValueAsString(features[0].getGeometry()));
//
//        template.batchUpdate(sql, batchParams.toArray(new SqlParameterSource[0]));
//    }
}
