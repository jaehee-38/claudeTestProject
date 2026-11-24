package com.example.firstproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 invest.* 설정. 실제 키 값은 .env 에서 들어옵니다.
 */
@ConfigurationProperties(prefix = "invest")
public record InvestProperties(DataGoKr dataGoKr, Koreaexim koreaexim, Kis kis, Frankfurter frankfurter) {

    public record DataGoKr(String serviceKey, String goldUrl, String aptTradeUrl,
                           String applyhomeUrl, String lhNoticeUrl) {
        public boolean configured() {
            return hasText(serviceKey);
        }
    }

    public record Koreaexim(String authKey, String url) {
        public boolean configured() {
            return hasText(authKey);
        }
    }

    /** 과거 환율 (유럽중앙은행 기준환율, 키 불필요) */
    public record Frankfurter(String url) {}

    public record Kis(String appKey, String appSecret, String accountNo, boolean mock,
                      String baseUrl, String tokenCacheFile) {
        public boolean configured() {
            return hasText(appKey) && hasText(appSecret);
        }

        public boolean accountConfigured() {
            return configured() && hasText(accountNo);
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
