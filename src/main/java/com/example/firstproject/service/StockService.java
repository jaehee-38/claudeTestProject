package com.example.firstproject.service;

import com.example.firstproject.common.ApiException;
import com.example.firstproject.common.Nums;
import com.example.firstproject.common.TtlCache;
import com.example.firstproject.config.InvestProperties;
import com.example.firstproject.dto.KisBalanceDto;
import com.example.firstproject.dto.StockQuoteDto;
import com.example.firstproject.service.external.KisTokenManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 한국투자증권 KIS Open API — 조회 전용.
 * 주문(매수/매도) API 는 일부러 구현하지 않았습니다.
 */
@Service
@RequiredArgsConstructor
public class StockService {

    private static final Pattern STOCK_CODE = Pattern.compile("^[0-9A-Z]{6}$");
    private static final String TR_PRICE = "FHKST01010100";
    private static final String TR_BALANCE_MOCK = "VTTC8434R";
    private static final String TR_BALANCE_REAL = "TTTC8434R";

    private final InvestProperties props;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final KisTokenManager tokenManager;

    private final TtlCache<String, StockQuoteDto> quoteCache = new TtlCache<>(Duration.ofSeconds(30));
    private final TtlCache<String, KisBalanceDto> balanceCache = new TtlCache<>(Duration.ofSeconds(30));

    private long lastCallAt = 0;

    public StockQuoteDto quote(String rawCode) {
        String code = normalizeCode(rawCode);
        return quoteCache.get(code, () -> fetchQuote(code));
    }

    public KisBalanceDto balance() {
        return balanceCache.get("balance", this::fetchBalance);
    }

    public static String normalizeCode(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase();
        if (!STOCK_CODE.matcher(code).matches()) {
            throw ApiException.badRequest("종목코드는 6자리여야 합니다 (예: 005930).");
        }
        return code;
    }

    private StockQuoteDto fetchQuote(String code) {
        JsonNode o = call("/uapi/domestic-stock/v1/quotations/inquire-price", TR_PRICE, Map.of(
                "FID_COND_MRKT_DIV_CODE", "J",
                "FID_INPUT_ISCD", code)).path("output");
        if (Nums.parse(o.path("stck_prpr").asText(null)) == null) {
            throw ApiException.notFound("종목 " + code + " 의 시세가 없습니다. 종목코드를 확인하세요.");
        }
        return new StockQuoteDto(
                code,
                o.path("bstp_kor_isnm").asText(null),
                num(o, "stck_prpr"), num(o, "prdy_vrss"), num(o, "prdy_ctrt"),
                num(o, "stck_oprc"), num(o, "stck_hgpr"), num(o, "stck_lwpr"),
                num(o, "acml_vol"), num(o, "w52_hgpr"), num(o, "w52_lwpr"),
                num(o, "per"), num(o, "pbr"));
    }

    private KisBalanceDto fetchBalance() {
        InvestProperties.Kis cfg = props.kis();
        if (!cfg.accountConfigured()) {
            throw ApiException.notConfigured("한국투자증권 계좌", "KIS_ACCOUNT_NO (예: 50012345-01)");
        }
        String digits = cfg.accountNo().replaceAll("[^0-9]", "");
        if (digits.length() == 8) {
            digits += "01"; // 상품코드를 생략하면 일반 위탁계좌(01)로 봅니다
        }
        if (digits.length() != 10) {
            throw ApiException.badRequest("KIS_ACCOUNT_NO 는 계좌번호 8자리 + 상품코드 2자리여야 합니다 (예: 50012345-01).");
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("CANO", digits.substring(0, 8));
        params.put("ACNT_PRDT_CD", digits.substring(8));
        params.put("AFHR_FLPR_YN", "N");
        params.put("OFL_YN", "");
        params.put("INQR_DVSN", "02");
        params.put("UNPR_DVSN", "01");
        params.put("FUND_STTL_ICLD_YN", "N");
        params.put("FNCG_AMT_AUTO_RDPT_YN", "N");
        params.put("PRCS_DVSN", "00");
        params.put("CTX_AREA_FK100", "");
        params.put("CTX_AREA_NK100", "");

        JsonNode res = call("/uapi/domestic-stock/v1/trading/inquire-balance",
                cfg.mock() ? TR_BALANCE_MOCK : TR_BALANCE_REAL, params);

        List<KisBalanceDto.Item> items = new ArrayList<>();
        for (JsonNode n : res.path("output1")) {
            var qty = num(n, "hldg_qty");
            if (qty == null || qty.signum() == 0) continue; // 오늘 전량 매도한 종목은 제외
            items.add(new KisBalanceDto.Item(
                    n.path("pdno").asText(), n.path("prdt_name").asText(),
                    qty, num(n, "pchs_avg_pric"), num(n, "prpr"),
                    num(n, "pchs_amt"), num(n, "evlu_amt"),
                    num(n, "evlu_pfls_amt"), num(n, "evlu_pfls_rt")));
        }
        JsonNode s = res.path("output2");
        if (s.isArray()) s = s.path(0);
        return new KisBalanceDto(cfg.mock(), items,
                num(s, "dnca_tot_amt"), num(s, "pchs_amt_smtl_amt"), num(s, "evlu_amt_smtl_amt"),
                num(s, "evlu_pfls_smtl_amt"), num(s, "nass_amt"));
    }

    /** GET 호출 공통: 토큰/헤더, 호출 간격, 토큰 만료 시 1회 재시도, rt_cd 확인 */
    private JsonNode call(String path, String trId, Map<String, String> params) {
        InvestProperties.Kis cfg = props.kis();
        if (!cfg.configured()) {
            throw ApiException.notConfigured("한국투자증권", "KIS_APP_KEY, KIS_APP_SECRET");
        }
        for (int attempt = 0; ; attempt++) {
            throttle(cfg.mock());
            StringBuilder q = new StringBuilder(cfg.baseUrl()).append(path).append('?');
            params.forEach((k, v) -> q.append(k).append('=').append(v).append('&'));
            String body = restClient.get()
                    .uri(URI.create(q.substring(0, q.length() - 1)))
                    .header("authorization", "Bearer " + tokenManager.getToken())
                    .header("appkey", cfg.appKey().trim())
                    .header("appsecret", cfg.appSecret().trim())
                    .header("tr_id", trId)
                    .header("custtype", "P")
                    .retrieve()
                    .onStatus(st -> true, (req, res) -> {})
                    .body(String.class);

            JsonNode n;
            try {
                n = objectMapper.readTree(body == null ? "{}" : body);
            } catch (Exception e) {
                throw ApiException.upstream("한국투자증권", "JSON 해석 실패");
            }
            String msgCd = n.path("msg_cd").asText("");
            if (msgCd.equals("EGW00123") && attempt == 0) { // 토큰 만료 → 재발급 후 재시도
                tokenManager.invalidate();
                continue;
            }
            if (!n.path("rt_cd").asText("").equals("0")) {
                String msg = n.path("msg1").asText(n.path("error_description").asText("알 수 없는 오류"));
                if (msgCd.equals("EGW00201")) msg += " (초당 호출 한도 초과, 잠시 후 다시 시도하세요)";
                throw ApiException.upstream("한국투자증권", msg.trim() + (msgCd.isEmpty() ? "" : " [" + msgCd + "]"));
            }
            return n;
        }
    }

    /** 모의투자는 초당 호출 한도가 낮아서 호출 사이에 간격을 둡니다 */
    private synchronized void throttle(boolean mock) {
        long minGap = mock ? 550 : 70;
        long wait = lastCallAt + minGap - System.currentTimeMillis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallAt = System.currentTimeMillis();
    }

    private static java.math.BigDecimal num(JsonNode n, String field) {
        return Nums.parse(n.path(field).asText(null));
    }
}
