package com.goods.market.trade.presentation;

import com.goods.market.common.api.ApiResponse;
import com.goods.market.common.auth.AuthPrincipal;
import com.goods.market.trade.application.TradeQueryService;
import com.goods.market.trade.application.dto.PurchaseHistoryItemDto;
import com.goods.market.trade.application.dto.SaleHistoryItemDto;
import com.goods.market.trade.presentation.dto.response.PurchaseHistoryResponse;
import com.goods.market.trade.presentation.dto.response.SaleHistoryResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeQueryService tradeQueryService;

    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<Slice<SaleHistoryResponse>>> getSaleHistory(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request,
            @RequestParam(name = "last_trade_id", required = false) Long lastTradeId,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Slice<SaleHistoryItemDto> sales = tradeQueryService.getSaleHistory(principal.memberId(), lastTradeId, size);
        return ResponseEntity.ok(ApiResponse.success(toSaleHistoryResponseSlice(sales), request.getRequestURI()));
    }

    @GetMapping("/purchases")
    public ResponseEntity<ApiResponse<Slice<PurchaseHistoryResponse>>> getPurchaseHistory(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request,
            @RequestParam(name = "last_trade_id", required = false) Long lastTradeId,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Slice<PurchaseHistoryItemDto> purchases = tradeQueryService.getPurchaseHistory(
                principal.memberId(),
                lastTradeId,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(toPurchaseHistoryResponseSlice(purchases), request.getRequestURI()));
    }

    private Slice<SaleHistoryResponse> toSaleHistoryResponseSlice(Slice<SaleHistoryItemDto> slice) {
        List<SaleHistoryResponse> responses = slice.getContent().stream()
                .map(SaleHistoryResponse::from)
                .toList();
        return new SliceImpl<>(
                responses,
                PageRequest.of(slice.getNumber(), slice.getSize(), slice.getSort()),
                slice.hasNext()
        );
    }

    private Slice<PurchaseHistoryResponse> toPurchaseHistoryResponseSlice(Slice<PurchaseHistoryItemDto> slice) {
        List<PurchaseHistoryResponse> responses = slice.getContent().stream()
                .map(PurchaseHistoryResponse::from)
                .toList();
        return new SliceImpl<>(
                responses,
                PageRequest.of(slice.getNumber(), slice.getSize(), slice.getSort()),
                slice.hasNext()
        );
    }

}
