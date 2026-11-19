package com.example.firstproject.controller.user;

import com.example.firstproject.dto.WatchItemDto;
import com.example.firstproject.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    private final WatchlistService watchlistService;

    public record WatchRequest(String code, String name) {}

    @GetMapping
    public List<WatchItemDto> list() {
        return watchlistService.list();
    }

    // 추가 (이미 있으면 이름만 바꿈): POST /api/watchlist {"code": "005930", "name": "삼성전자"}
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchItemDto add(@RequestBody WatchRequest req) {
        return watchlistService.add(req == null ? null : req.code(), req == null ? null : req.name());
    }

    // 삭제: DELETE /api/watchlist/005930
    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String code) {
        watchlistService.remove(code);
    }
}
