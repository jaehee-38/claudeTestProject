package com.example.firstproject.dto;

import com.example.firstproject.entity.WatchItem;

import java.time.LocalDateTime;

public record WatchItemDto(String code, String name, LocalDateTime createdAt) {

    public static WatchItemDto from(WatchItem w) {
        return new WatchItemDto(w.getCode(), w.getName(), w.getCreatedAt());
    }
}
