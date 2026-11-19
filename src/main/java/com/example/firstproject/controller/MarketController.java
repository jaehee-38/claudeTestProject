package com.example.firstproject.controller;

import com.example.firstproject.config.InvestProperties;
import com.example.firstproject.dto.AptTradeDto;
import com.example.firstproject.dto.ExchangeRateDto;
import com.example.firstproject.dto.GoldPriceDto;
import com.example.firstproject.dto.KisBalanceDto;
import com.example.firstproject.dto.StockQuoteDto;
import com.example.firstproject.service.ExchangeService;
import com.example.firstproject.service.GoldService;
import com.example.firstproject.service.RealEstateService;
import com.example.firstproject.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 외부 시세 조회 API (모두 GET, 조회 전용).
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MarketController {

    private final InvestProperties props;
    private final GoldService goldService;
    private final ExchangeService exchangeService;
    private final StockService stockService;
    private final RealEstateService realEstateService;

    /** 어떤 API 키가 설정됐는지 (키 값은 내려주지 않음) */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "dataGoKr", props.dataGoKr().configured(),
                "koreaexim", props.koreaexim().configured(),
                "kis", props.kis().configured(),
                "kisAccount", props.kis().accountConfigured(),
                "kisMock", props.kis().mock());
    }

    // GET /api/gold?days=30
    @GetMapping("/gold")
    public List<GoldPriceDto> gold(@RequestParam(defaultValue = "30") int days) {
        return goldService.recent(days);
    }

    // GET /api/exchange
    @GetMapping("/exchange")
    public ExchangeRateDto exchange() {
        return exchangeService.latest();
    }

    // GET /api/stocks/quote?code=005930
    @GetMapping("/stocks/quote")
    public StockQuoteDto quote(@RequestParam String code) {
        return stockService.quote(code);
    }

    // GET /api/stocks/balance  (한국투자증권 계좌 잔고)
    @GetMapping("/stocks/balance")
    public KisBalanceDto balance() {
        return stockService.balance();
    }

    // GET /api/real-estate/apt-trades?lawdCd=11680&dealYm=202410
    @GetMapping("/real-estate/apt-trades")
    public List<AptTradeDto> aptTrades(@RequestParam String lawdCd, @RequestParam String dealYm) {
        return realEstateService.aptTrades(lawdCd, dealYm);
    }
}
