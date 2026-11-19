package com.example.firstproject.common;

import java.math.BigDecimal;

public final class Nums {

    private Nums() {}

    /** "1,385.50", " 82,500 ", "" 같은 외부 API 숫자 문자열을 BigDecimal 로. 비어 있거나 이상하면 null. */
    public static BigDecimal parse(String s) {
        if (s == null) return null;
        String cleaned = s.replace(",", "").trim();
        if (cleaned.isEmpty() || cleaned.equals("-")) return null;
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
