package com.example.firstproject.service.external;

import com.example.firstproject.common.ApiException;
import com.example.firstproject.config.InvestProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

/**
 * 한국투자증권 접근토큰 관리.
 * 토큰은 24시간 유효하지만 발급은 1분에 1번 정도로 제한되고, 자주 발급하면 막힐 수 있습니다.
 * 그래서 메모리 + 파일(.kis-token.json)에 저장해 두고 서버를 재시작해도 재사용합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisTokenManager {

    /** 만료 직전 토큰은 쓰지 않습니다 */
    private static final Duration SAFETY_MARGIN = Duration.ofMinutes(10);

    private final InvestProperties props;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private String token;
    private Instant expiresAt = Instant.EPOCH;

    public synchronized String getToken() {
        if (isValid()) return token;
        if (loadFromFile() && isValid()) return token;
        issue();
        return token;
    }

    /** API 가 토큰 만료(EGW00123) 를 알려줬을 때 호출 */
    public synchronized void invalidate() {
        token = null;
        expiresAt = Instant.EPOCH;
        try {
            Files.deleteIfExists(cacheFile());
        } catch (Exception ignored) {
        }
    }

    private boolean isValid() {
        return token != null && Instant.now().plus(SAFETY_MARGIN).isBefore(expiresAt);
    }

    private void issue() {
        InvestProperties.Kis cfg = props.kis();
        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", cfg.appKey().trim(),
                "appsecret", cfg.appSecret().trim());
        JsonNode res;
        try {
            res = restClient.post()
                    .uri(cfg.baseUrl() + "/oauth2/tokenP")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw ApiException.upstream("한국투자증권 토큰 발급", KisErrors.describe(e.getResponseBodyAsString(), objectMapper));
        }
        if (res == null || !res.hasNonNull("access_token")) {
            throw ApiException.upstream("한국투자증권 토큰 발급", "access_token 이 없습니다");
        }
        token = res.get("access_token").asText();
        expiresAt = Instant.now().plusSeconds(res.path("expires_in").asLong(86_400));
        log.info("KIS 접근토큰 새로 발급 (만료: {})", expiresAt);
        saveToFile();
    }

    private Path cacheFile() {
        return Path.of(props.kis().tokenCacheFile());
    }

    /** 앱키가 바뀌면 예전 토큰을 쓰지 않도록 앱키 지문을 같이 저장합니다 (앱키 자체는 저장하지 않음) */
    private String keyFingerprint() {
        try {
            InvestProperties.Kis cfg = props.kis();
            byte[] d = MessageDigest.getInstance("SHA-256")
                    .digest((cfg.appKey().trim() + "|" + cfg.baseUrl()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d, 0, 8);
        } catch (Exception e) {
            return "";
        }
    }

    private boolean loadFromFile() {
        Path f = cacheFile();
        if (!Files.exists(f)) return false;
        try {
            JsonNode n = objectMapper.readTree(f.toFile());
            if (!keyFingerprint().equals(n.path("key").asText())) return false;
            token = n.path("token").asText(null);
            expiresAt = Instant.ofEpochSecond(n.path("expiresAt").asLong(0));
            return token != null;
        } catch (Exception e) {
            log.warn("KIS 토큰 캐시 파일을 읽지 못했습니다: {}", e.getMessage());
            return false;
        }
    }

    /** 토큰 파일은 본인만 읽을 수 있게 (rw-------). Windows 처럼 POSIX 권한이 없으면 건너뜀 */
    private static void restrictToOwner(Path file) {
        try {
            Files.setPosixFilePermissions(file, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | java.io.IOException ignored) {
        }
    }

    private void saveToFile() {
        try {
            objectMapper.writeValue(cacheFile().toFile(), Map.of(
                    "key", keyFingerprint(),
                    "token", token,
                    "expiresAt", expiresAt.getEpochSecond()));
            restrictToOwner(cacheFile());
        } catch (Exception e) {
            log.warn("KIS 토큰 캐시 파일을 저장하지 못했습니다: {}", e.getMessage());
        }
    }
}
