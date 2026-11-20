package com.example.firstproject.controller.user;

import com.example.firstproject.dto.*;
import com.example.firstproject.service.InvestService;
import com.example.firstproject.dto.InvestAssetRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invest")
@RequiredArgsConstructor
public class InvestController {

    private final InvestService investService;

    // GET /api/invest/assets
    @GetMapping("/assets")
    public List<InvestAssetRes> getAssets() {
        return investService.getTradableAssets();
    }

    // POST /api/invest/accounts
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public InvestAccountRes createAccount(@RequestBody InvestAccountCreateReq req) {
        return investService.createAccount(req);
    }

    // GET /api/invest/accounts/{accountNo}
    @GetMapping("/accounts/{accountNo}")
    public InvestAccountRes getAccount(@PathVariable Long accountNo) {
        return investService.getAccount(accountNo);
    }

    // GET /api/invest/accounts?memberNo=1
    @GetMapping("/accounts")
    public List<InvestAccountRes> getAccountsByMember(@RequestParam Long memberNo) {
        return investService.getAccountsByMember(memberNo);
    }

    // POST /api/invest/accounts/{accountNo}/orders
    @PostMapping("/accounts/{accountNo}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public InvestOrderRes placeOrder(@PathVariable Long accountNo,
                                      @RequestBody InvestOrderReq req) {
        return investService.placeOrder(accountNo, req);
    }

    // GET /api/invest/accounts/{accountNo}/orders
    @GetMapping("/accounts/{accountNo}/orders")
    public List<InvestOrderRes> getOrders(@PathVariable Long accountNo) {
        return investService.getOrders(accountNo);
    }

    // GET /api/invest/accounts/{accountNo}/holdings
    @GetMapping("/accounts/{accountNo}/holdings")
    public List<InvestHoldingRes> getHoldings(@PathVariable Long accountNo) {
        return investService.getHoldings(accountNo);
    }

    // GET /api/invest/accounts/{accountNo}/ledger
    @GetMapping("/accounts/{accountNo}/ledger")
    public List<InvestLedgerRes> getLedger(@PathVariable Long accountNo) {
        return investService.getLedger(accountNo);
    }
}
