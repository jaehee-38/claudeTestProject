package com.example.firstproject.service.external;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 공공데이터포털(data.go.kr) 공통 처리.
 */
public final class DataGoKr {

    private DataGoKr() {}

    // 오류 응답은 XML(<errMsg>…) 또는 JSON("errMsg": "…") 으로 옵니다.
    private static final Pattern ERR_MSG = Pattern.compile("(?:<errMsg>|\"errMsg\"\\s*:\\s*\")([^<\"]*)");
    private static final Pattern AUTH_MSG = Pattern.compile("(?:<returnAuthMsg>|\"returnAuthMsg\"\\s*:\\s*\")([^<\"]*)");
    private static final Pattern RESULT_MSG = Pattern.compile("(?:<resultMsg>|\"resultMsg\"\\s*:\\s*\")([^<\"]*)");

    /** 공공데이터포털 게이트웨이 오류 응답인지 (인증키 오류, 호출 한도 초과 등) */
    public static boolean isGatewayError(String body) {
        return body != null && body.contains("OpenAPI_ServiceResponse");
    }

    /**
     * 서비스키를 URL 에 한 번만 인코딩된 형태로 만듭니다.
     * Decoding 키(+,/,= 포함)는 인코딩하고, 이미 %가 들어간 Encoding 키는 그대로 씁니다.
     * (두 번 인코딩되면 SERVICE_KEY_IS_NOT_REGISTERED_ERROR 가 납니다)
     */
    public static String encodeKey(String key) {
        String k = key.trim();
        return k.contains("%") ? k : URLEncoder.encode(k, StandardCharsets.UTF_8);
    }

    /**
     * 정상 응답이 아닐 때(인증 오류 XML, 게이트웨이 텍스트 등) 사람이 읽을 메시지를 뽑습니다.
     */
    public static String describeError(String body) {
        if (body == null || body.isBlank()) return "빈 응답";
        Matcher err = ERR_MSG.matcher(body);
        Matcher auth = AUTH_MSG.matcher(body);
        if (err.find()) {
            String code = err.group(1).trim();
            return hint(auth.find() ? auth.group(1).trim() + " [" + code + "]" : code);
        }
        if (auth.find()) return hint(auth.group(1).trim());
        Matcher m = RESULT_MSG.matcher(body);
        if (m.find()) return hint(m.group(1).trim());
        String trimmed = body.strip();
        return hint(trimmed.length() > 200 ? trimmed.substring(0, 200) + "…" : trimmed);
    }

    private static String hint(String msg) {
        if (msg.contains("SERVICE_KEY_IS_NOT_REGISTERED") || msg.contains("Unauthorized")) {
            return msg + " (인증키가 틀렸거나, 이 API를 활용신청하지 않았거나, 발급 직후라 아직 활성화 전일 수 있습니다. 보통 1시간 정도 걸립니다)";
        }
        if (msg.contains("LIMITED_NUMBER_OF_SERVICE_REQUESTS")) {
            return msg + " (오늘 호출 한도를 다 썼습니다)";
        }
        return msg;
    }
}
