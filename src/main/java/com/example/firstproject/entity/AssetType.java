package com.example.firstproject.entity;

/**
 * 보유 자산 종류와 각 종류의 수량/가격 단위.
 */
public enum AssetType {
    /** 국내주식. code = 종목코드 6자리, 수량 = 주, 현재가 = KIS 시세 */
    STOCK("주식"),
    /** 금. 수량 = g, 현재가 = KRX 금 99.99_1Kg 종가(원/g) */
    GOLD("금"),
    /** 외화. code = 통화코드(USD 등), 수량 = 외화 금액, 현재가 = 수출입은행 매매기준율 */
    CURRENCY("외화"),
    /** 부동산. 수량 = 1, 현재가는 직접 입력 */
    REAL_ESTATE("부동산"),
    /** 그 외 (예금, 코인 등). 현재가는 직접 입력 */
    ETC("기타");

    private final String label;

    AssetType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
