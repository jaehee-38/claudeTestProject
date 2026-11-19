package com.example.firstproject.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관심종목 (국내주식 종목코드).
 */
@Entity
@Table(name = "watch_item", uniqueConstraints = @UniqueConstraint(name = "uk_watch_item_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 6)
    private String code;

    /** 표시 이름 (선택). KIS 시세 API 가 종목명을 주지 않아서 직접 붙입니다 */
    @Column(length = 100)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public WatchItem(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public void rename(String name) {
        this.name = name;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
