package com.example.firstproject.controller.user;

import com.example.firstproject.config.InvestProperties;
import com.example.firstproject.dto.AptTradeDto;
import com.example.firstproject.dto.ExchangeRateDto;
import com.example.firstproject.dto.FxHistoryDto;
import com.example.firstproject.dto.GoldHistoryDto;
import com.example.firstproject.dto.GoldPriceDto;
import com.example.firstproject.dto.KisBalanceDto;
import com.example.firstproject.dto.StockQuoteDto;
import com.example.firstproject.dto.SubscriptionListRes;
import com.example.firstproject.service.ExchangeService;
import com.example.firstproject.service.FxHistoryService;
import com.example.firstproject.service.GoldService;
import com.example.firstproject.service.RealEstateService;
import com.example.firstproject.service.StockService;
import com.example.firstproject.service.SubscriptionService;
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
    private final FxHistoryService fxHistoryService;
    private final StockService stockService;
    private final RealEstateService realEstateService;
    private final SubscriptionService subscriptionService;

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

    // GET /api/gold/history?from=2026-08-24&to=2026-11-24  (금 1kg 종목, 그래프용)
    @GetMapping("/gold/history")
    public GoldHistoryDto goldHistory(@RequestParam String from, @RequestParam(required = false) String to) {
        return goldService.history(from, to);
    }

    // GET /api/exchange
    @GetMapping("/exchange")
    public ExchangeRateDto exchange() {
        return exchangeService.latest();
    }

    // GET /api/exchange/history?from=2026-08-24&to=2026-11-24  (주요 통화, 그래프용)
    @GetMapping("/exchange/history")
    public FxHistoryDto exchangeHistory(@RequestParam String from, @RequestParam(required = false) String to) {
        return fxHistoryService.history(from, to);
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

    // GET /api/subscriptions  (청약홈 + LH 청약 공고, 최근 60일)
    @GetMapping("/subscriptions")
    public SubscriptionListRes subscriptions() {
        return subscriptionService.notices();
    }
}
