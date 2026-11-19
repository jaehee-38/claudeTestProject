package com.example.firstproject.controller.user;

import com.example.firstproject.dto.HoldingRequest;
import com.example.firstproject.dto.PortfolioDto;
import com.example.firstproject.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 내 투자 현황 (직접 등록한 자산).
 */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    // 전체 + 현재 시세 평가: GET /api/portfolio
    @GetMapping
    public PortfolioDto summary() {
        return portfolioService.summary();
    }

    // 등록: POST /api/portfolio/holdings
    @PostMapping("/holdings")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> create(@RequestBody HoldingRequest request) {
        return Map.of("id", portfolioService.create(request));
    }

    // 수정: PUT /api/portfolio/holdings/{id}
    @PutMapping("/holdings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable Long id, @RequestBody HoldingRequest request) {
        portfolioService.update(id, request);
    }

    // 삭제: DELETE /api/portfolio/holdings/{id}
    @DeleteMapping("/holdings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        portfolioService.delete(id);
    }
}
