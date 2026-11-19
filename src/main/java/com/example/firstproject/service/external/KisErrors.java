package com.example.firstproject.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class KisErrors {

    private KisErrors() {}

    /** KIS 오류 본문 {"msg_cd": "...", "msg1": "..."} 또는 {"error_code", "error_description"} 를 문장으로 */
    static String describe(String body, ObjectMapper om) {
        if (body == null || body.isBlank()) return "빈 응답";
        try {
            JsonNode n = om.readTree(body);
            String code = n.path("msg_cd").asText(n.path("error_code").asText(""));
            String msg = n.path("msg1").asText(n.path("error_description").asText(""));
            String text = (msg + (code.isEmpty() ? "" : " [" + code + "]")).trim();
            if (code.equals("EGW00133")) {
                text += " (토큰은 1분에 한 번만 발급됩니다. 1분 뒤 다시 시도하세요)";
            } else if (code.equals("EGW00201")) {
                text += " (초당 호출 한도 초과)";
            }
            return text.isEmpty() ? body : text;
        } catch (Exception e) {
            return body.length() > 200 ? body.substring(0, 200) + "…" : body;
        }
    }
}
