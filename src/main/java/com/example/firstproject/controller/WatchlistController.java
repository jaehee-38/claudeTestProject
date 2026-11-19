package com.example.firstproject.controller;

import com.example.firstproject.common.ApiException;
import com.example.firstproject.entity.WatchItem;
import com.example.firstproject.repository.WatchItemRepository;
import com.example.firstproject.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관심종목 목록. 시세는 프론트에서 /api/stocks/quote 로 종목마다 따로 불러옵니다
 * (한 종목이 느려도 나머지가 먼저 보이도록).
 */
@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private static final int MAX_ITEMS = 30;

    private final WatchItemRepository repository;

    public record WatchItemDto(String code, String name) {}

    public record WatchRequest(String code, String name) {}

    @GetMapping
    public List<WatchItemDto> list() {
        return repository.findAllByOrderByIdAsc().stream()
                .map(w -> new WatchItemDto(w.getCode(), w.getName()))
                .toList();
    }

    // 추가 (이미 있으면 이름만 바꿈): POST /api/watchlist {"code": "005930", "name": "삼성전자"}
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public WatchItemDto add(@RequestBody WatchRequest req) {
        String code = StockService.normalizeCode(req == null ? null : req.code());
        String name = req.name() == null || req.name().isBlank() ? null : req.name().trim();
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
        return new WatchItemDto(item.getCode(), item.getName());
    }

    // 삭제: DELETE /api/watchlist/005930
    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void remove(@PathVariable String code) {
        repository.findByCode(StockService.normalizeCode(code)).ifPresent(repository::delete);
    }
}
