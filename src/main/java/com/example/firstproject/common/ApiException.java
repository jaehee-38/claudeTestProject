package com.example.firstproject.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 화면에 그대로 보여줄 메시지를 담는 예외. GlobalExceptionHandler 가 {"message": ...} 로 내려줍니다.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException notConfigured(String what, String envNames) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                what + " API 키가 설정되지 않았습니다. .env 에 " + envNames + " 값을 넣고 서버를 다시 실행하세요.");
    }

    public static ApiException upstream(String source, String detail) {
        return new ApiException(HttpStatus.BAD_GATEWAY, source + " 응답 오류: " + detail);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }
}
