package com.example.firstproject.service;

import com.example.firstproject.common.ApiException;
import com.example.firstproject.dto.ExchangeRateDto;
import com.example.firstproject.dto.GoldPriceDto;
import com.example.firstproject.dto.HoldingDto;
import com.example.firstproject.dto.HoldingRequest;
import com.example.firstproject.dto.PortfolioDto;
import com.example.firstproject.entity.AssetType;
import com.example.firstproject.entity.Holding;
import com.example.firstproject.repository.HoldingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final HoldingRepository holdingRepository;
    private final StockService stockService;
    private final GoldService goldService;
    private final ExchangeService exchangeService;

    // ---------- 조회 + 평가 ----------

    /** DB 조회만 트랜잭션 안에서 하고, 외부 시세 호출은 트랜잭션 밖에서 합니다 (DB 커넥션을 오래 잡지 않도록) */
    public PortfolioDto summary() {
        List<Holding> holdings = holdingRepository.findAllByOrderByAssetTypeAscIdAsc();

        List<PortfolioDto.Item> items = new ArrayList<>();
        Map<AssetType, BigDecimal> byType = new EnumMap<>(AssetType.class);
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        int unpriced = 0;

        // 같은 요청 안에서 금·환율 실패를 한 번만 겪도록 결과를 기억합니다.
        PriceContext ctx = new PriceContext();

        for (Holding h : holdings) {
            Price p = price(h, ctx);
            BigDecimal cost = h.getQuantity().multiply(h.getAveragePrice());
            BigDecimal value = null, profit = null, rate = null;
            if (p.value() != null) {
                value = h.getQuantity().multiply(p.value()).setScale(0, RoundingMode.HALF_UP);
                profit = value.subtract(cost).setScale(0, RoundingMode.HALF_UP);
                rate = percent(profit, cost);
                totalCost = totalCost.add(cost);
                totalValue = totalValue.add(value);
                byType.merge(h.getAssetType(), value, BigDecimal::add);
            } else {
                unpriced++;
            }
            items.add(new PortfolioDto.Item(
                    h.getId(), h.getAssetType(), h.getAssetType().getLabel(), h.getName(), h.getCode(),
                    h.getQuantity().stripTrailingZeros(), h.getAveragePrice().stripTrailingZeros(),
                    h.getManualPrice() == null ? null : h.getManualPrice().stripTrailingZeros(),
                    h.getMemo(), p.value(), p.source(), p.error(),
                    cost.setScale(0, RoundingMode.HALF_UP), value, profit, rate));
        }

        BigDecimal total = totalValue;
        List<PortfolioDto.Allocation> allocation = byType.entrySet().stream()
                .map(e -> new PortfolioDto.Allocation(e.getKey(), e.getKey().getLabel(), e.getValue(),
                        total.signum() == 0 ? BigDecimal.ZERO
                                : e.getValue().multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP)))
                .toList();

        BigDecimal totalProfit = totalValue.subtract(totalCost).setScale(0, RoundingMode.HALF_UP);
        return new PortfolioDto(items, totalCost.setScale(0, RoundingMode.HALF_UP), totalValue, totalProfit,
                percent(totalProfit, totalCost), allocation, unpriced);
    }

    private record Price(BigDecimal value, String source, String error) {
        static Price ok(BigDecimal v, String src) { return new Price(v, src, null); }
        static Price fail(String err) { return new Price(null, null, err); }
    }

    private static class PriceContext {
        Optional<GoldPriceDto> gold;
        String goldError;
        ExchangeRateDto rates;
        String ratesError;
    }

    private Price price(Holding h, PriceContext ctx) {
        // 직접 입력한 현재가가 있으면 그걸 우선합니다.
        if (h.getManualPrice() != null) {
            return Price.ok(h.getManualPrice(), "직접 입력");
        }
        try {
            return switch (h.getAssetType()) {
                case STOCK -> {
                    var q = stockService.quote(h.getCode());
                    yield Price.ok(q.price(), "한국투자증권");
                }
                case GOLD -> {
                    if (ctx.gold == null && ctx.goldError == null) {
                        try {
                            ctx.gold = goldService.latestMain();
                        } catch (ApiException e) {
                            ctx.goldError = e.getMessage();
                        }
                    }
                    if (ctx.goldError != null) yield Price.fail(ctx.goldError);
                    yield ctx.gold
                            .map(g -> Price.ok(g.close(), "KRX 금시장 " + g.baseDate()))
                            .orElse(Price.fail("최근 금 시세가 없습니다"));
                }
                case CURRENCY -> {
                    if (ctx.rates == null && ctx.ratesError == null) {
                        try {
                            ctx.rates = exchangeService.latest();
                        } catch (ApiException e) {
                            ctx.ratesError = e.getMessage();
                        }
                    }
                    if (ctx.ratesError != null) yield Price.fail(ctx.ratesError);
                    String code = h.getCode() == null ? "" : h.getCode().trim().toUpperCase();
                    yield ctx.rates.rates().stream()
                            .filter(r -> r.code().equals(code))
                            .findFirst()
                            .map(r -> Price.ok(r.perOneUnit(), "수출입은행 " + ctx.rates.date()))
                            .orElse(Price.fail("수출입은행 환율에 " + code + " 통화가 없습니다"));
                }
                case REAL_ESTATE, ETC -> Price.fail("현재가를 직접 입력하세요");
            };
        } catch (ApiException e) {
            return Price.fail(e.getMessage());
        }
    }

    private static BigDecimal percent(BigDecimal profit, BigDecimal cost) {
        if (cost == null || cost.signum() == 0) return null;
        return profit.multiply(HUNDRED).divide(cost, 2, RoundingMode.HALF_UP);
    }

    // ---------- 원본 목록 (관리자 화면) ----------

    /** 시세를 붙이지 않은 DB 원본 목록. 외부 API 를 부르지 않아서 빠릅니다 */
    @Transactional(readOnly = true)
    public List<HoldingDto> listRaw() {
        return holdingRepository.findAllByOrderByAssetTypeAscIdAsc().stream().map(HoldingDto::from).toList();
    }

    @Transactional(readOnly = true)
    public long count() {
        return holdingRepository.count();
    }

    // ---------- 등록 / 수정 / 삭제 ----------

    @Transactional
    public Long create(HoldingRequest req) {
        Valid v = validate(req);
        Holding h = new Holding(v.type, v.name, v.code, v.quantity, v.averagePrice, v.manualPrice, v.memo);
        return holdingRepository.save(h).getId();
    }

    @Transactional
    public void update(Long id, HoldingRequest req) {
        Valid v = validate(req);
        get(id).update(v.type, v.name, v.code, v.quantity, v.averagePrice, v.manualPrice, v.memo);
    }

    @Transactional
    public void delete(Long id) {
        holdingRepository.delete(get(id));
    }

    private Holding get(Long id) {
        return holdingRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("자산을 찾을 수 없습니다: " + id));
    }

    private record Valid(AssetType type, String name, String code, BigDecimal quantity,
                         BigDecimal averagePrice, BigDecimal manualPrice, String memo) {}

    private Valid validate(HoldingRequest r) {
        if (r == null || r.assetType() == null) throw ApiException.badRequest("자산 종류를 선택하세요.");
        String name = trimToNull(r.name());
        if (name == null) throw ApiException.badRequest("이름을 입력하세요.");
        if (name.length() > 100) throw ApiException.badRequest("이름은 100자 이하여야 합니다.");
        if (r.quantity() == null || r.quantity().signum() <= 0) throw ApiException.badRequest("수량은 0보다 커야 합니다.");
        if (r.averagePrice() == null || r.averagePrice().signum() < 0) throw ApiException.badRequest("매입가를 입력하세요.");
        if (r.manualPrice() != null && r.manualPrice().signum() < 0) throw ApiException.badRequest("현재가는 0 이상이어야 합니다.");
        String memo = trimToNull(r.memo());
        if (memo != null && memo.length() > 255) throw ApiException.badRequest("메모는 255자 이하여야 합니다.");

        String code = trimToNull(r.code());
        switch (r.assetType()) {
            case STOCK -> code = StockService.normalizeCode(code);
            case CURRENCY -> {
                if (code == null || !code.matches("(?i)^[A-Z]{3}$")) {
                    throw ApiException.badRequest("통화코드는 영문 3자리여야 합니다 (예: USD).");
                }
                code = code.toUpperCase();
            }
            case GOLD, REAL_ESTATE, ETC -> code = null;
        }
        return new Valid(r.assetType(), name, code, r.quantity(), r.averagePrice(), r.manualPrice(), memo);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
