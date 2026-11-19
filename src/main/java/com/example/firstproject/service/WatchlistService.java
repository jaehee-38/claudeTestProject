package com.example.firstproject.service;

import com.example.firstproject.common.ApiException;
import com.example.firstproject.dto.WatchItemDto;
import com.example.firstproject.entity.WatchItem;
import com.example.firstproject.repository.WatchItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관심종목. 사용자 화면(/api/watchlist)과 관리자 화면(/admin/api/watchlist)이 같이 씁니다.
 */
@Service
@RequiredArgsConstructor
public class WatchlistService {

    public static final int MAX_ITEMS = 30;

    private final WatchItemRepository repository;

    @Transactional(readOnly = true)
    public List<WatchItemDto> list() {
        return repository.findAllByOrderByIdAsc().stream().map(WatchItemDto::from).toList();
    }

    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    /** 추가 (이미 있으면 이름만 바꿈) */
    @Transactional
    public WatchItemDto add(String rawCode, String rawName) {
        String code = StockService.normalizeCode(rawCode);
        String name = rawName == null || rawName.isBlank() ? null : rawName.trim();
        if (name != null && name.length() > 100) throw ApiException.badRequest("이름은 100자 이하여야 합니다.");

        WatchItem item = repository.findByCode(code).orElse(null);
        if (item == null) {
            if (repository.count() >= MAX_ITEMS) {
                throw ApiException.badRequest("관심종목은 " + MAX_ITEMS + "개까지 등록할 수 있습니다.");
            }
            item = repository.save(new WatchItem(code, name));
        } else if (name != null) {
            item.rename(name);
        }
        return WatchItemDto.from(item);
    }

    @Transactional
    public void remove(String code) {
        repository.findByCode(StockService.normalizeCode(code)).ifPresent(repository::delete);
    }
}
