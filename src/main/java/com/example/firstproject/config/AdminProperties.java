package com.example.firstproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 admin.* 설정. 비밀번호는 .env 의 ADMIN_PASSWORD 에서 들어옵니다.
 */
@ConfigurationProperties(prefix = "admin")
public record AdminProperties(String username, String password) {

    public boolean configured() {
        return password != null && !password.isBlank();
    }
}
