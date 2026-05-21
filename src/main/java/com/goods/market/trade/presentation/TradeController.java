package com.goods.market.trade.presentation;

import com.goods.market.trade.application.TradeCommandService;
import com.goods.market.trade.application.TradeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
public class TradeController {

    private final TradeCommandService tradeCommandService;
    private final TradeQueryService tradeQueryService;



}
