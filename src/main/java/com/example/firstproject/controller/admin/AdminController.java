package com.example.firstproject.controller.admin;

import com.example.firstproject.config.InvestProperties;
import com.example.firstproject.dto.HoldingDto;
import com.example.firstproject.dto.WatchItemDto;
import com.example.firstproject.repository.InvestAccountRepository;
import com.example.firstproject.repository.MemberRepository;
import com.example.firstproject.service.PortfolioService;
import com.example.firstproject.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자 API (/admin/api/**). 서비스는 사용자 컨트롤러와 같은 것을 씁니다.
 * /admin/** 전체는 AdminAuthFilter 가 비밀번호로 막고 있습니다.
 */
@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
public class AdminController {

    private final InvestProperties props;
    private final PortfolioService portfolioService;
    private final WatchlistService watchlistService;
    private final MemberRepository memberRepo;
    private final InvestAccountRepository investAccountRepo;

    // 개요: GET /admin/api/overview
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Map<String, Object> keys = new LinkedHashMap<>();
        keys.put("dataGoKr", props.dataGoKr().configured());
        keys.put("koreaexim", props.koreaexim().configured());
        keys.put("kis", props.kis().configured());
        keys.put("kisAccount", props.kis().accountConfigured());
        keys.put("kisMock", props.kis().mock());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("holdingCount", portfolioService.count());
        result.put("watchCount", watchlistService.count());
        result.put("watchMax", WatchlistService.MAX_ITEMS);
        result.put("memberCount", memberRepo.count());
        result.put("investAccountCount", investAccountRepo.count());
        result.put("apiKeys", keys);
        return result;
    }

    // 보유 자산 원본 목록: GET /admin/api/holdings
    @GetMapping("/holdings")
    public List<HoldingDto> holdings() {
        return portfolioService.listRaw();
    }

    // 보유 자산 삭제: DELETE /admin/api/holdings/{id}
    @DeleteMapping("/holdings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHolding(@PathVariable Long id) {
        portfolioService.delete(id);
    }

    // 관심종목 목록: GET /admin/api/watchlist
    @GetMapping("/watchlist")
    public List<WatchItemDto> watchlist() {
        return watchlistService.list();
    }

    // 관심종목 삭제: DELETE /admin/api/watchlist/{code}
    @DeleteMapping("/watchlist/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWatch(@PathVariable String code) {
        watchlistService.remove(code);
    }
}
