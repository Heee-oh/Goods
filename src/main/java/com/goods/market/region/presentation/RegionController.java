package com.goods.market.region.presentation;

import com.goods.market.region.infrastructure.RegionJpaRepository;
import com.goods.market.region.presentation.dto.response.RegionSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionJpaRepository regionJpaRepository;

    @GetMapping("/search")
    public List<RegionSearchResponse> search(@RequestParam(name = "query", defaultValue = "") String query) {
        return regionJpaRepository.searchByKeyword(query.trim()).stream()
                .map(RegionSearchResponse::from)
                .toList();
    }

    @GetMapping("/nearby")
    public List<RegionSearchResponse> nearby(
            @RequestParam("lat") BigDecimal lat,
            @RequestParam("lng") BigDecimal lng
    ) {
        return regionJpaRepository.findNearby(lat, lng).stream()
                .map(RegionSearchResponse::from)
                .toList();
    }
}
