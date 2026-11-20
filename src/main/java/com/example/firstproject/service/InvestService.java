package com.example.firstproject.service;

import com.example.firstproject.common.ApiException;
import com.example.firstproject.dto.*;
import com.example.firstproject.entity.*;
import com.example.firstproject.dto.InvestAssetRes;
import com.example.firstproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class InvestService {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.0005"); // 0.05%
    private static final BigDecimal TAX_RATE = new BigDecimal("0.002");  // 0.2% (매도 양도세)

    private final InvestAccountRepository accountRepo;
    private final InvestAssetRepository assetRepo;
    private final InvestOrderRepository orderRepo;
    private final InvestExecutionRepository execRepo;
    private final InvestHoldingRepository holdingRepo;
    private final AccountLedgerRepository ledgerRepo;

    // ── 자산 목록 ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<InvestAssetRes> getTradableAssets() {
        return assetRepo.findByTradableYnOrderByAssetTypeCdAscAssetNoAsc("Y").stream()
                .map(a -> InvestAssetRes.of(a,
                        assetRepo.findRealtimePrice(a.getAssetNo())
                                .or(() -> assetRepo.findLatestDailyPrice(a.getAssetNo()))
                                .orElse(null)))
                .toList();
    }

    // ── 계좌 개설 ─────────────────────────────────────────────────────────────

    public InvestAccountRes createAccount(InvestAccountCreateReq req) {
        if (req.initAmt() == null || req.initAmt().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "초기금액은 0보다 커야 합니다.");
        }
        InvestAccount account = InvestAccount.open(req.memberNo(), req.accountNm(), req.initAmt(), "system");
        accountRepo.save(account);

        ledgerRepo.save(AccountLedger.of(
                account.getAccountNo(), "DEPOSIT", req.initAmt(), req.initAmt(),
                "ACCOUNT", account.getAccountNo(), "계좌 개설 초기금액 입금"
        ));
        return InvestAccountRes.from(account);
    }

    @Transactional(readOnly = true)
    public InvestAccountRes getAccount(Long accountNo) {
        return InvestAccountRes.from(findAccount(accountNo));
    }

    @Transactional(readOnly = true)
    public List<InvestAccountRes> getAccountsByMember(Long memberNo) {
        return accountRepo.findByMemberNo(memberNo).stream()
                .map(InvestAccountRes::from).toList();
    }

    // ── 주문 ─────────────────────────────────────────────────────────────────

    public InvestOrderRes placeOrder(Long accountNo, InvestOrderReq req) {
        validateOrderReq(req);

        InvestAccount account = findAccount(accountNo);
        if (!"ACTIVE".equals(account.getAccountStatCd())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "비활성 계좌입니다.");
        }

        InvestAsset asset = assetRepo.findById(req.assetNo())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "자산을 찾을 수 없습니다."));
        if (!"Y".equals(asset.getTradableYn())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "거래 불가능한 자산입니다.");
        }

        BigDecimal price = currentPrice(req.assetNo());
        BigDecimal execAmt = price.multiply(req.qty()).setScale(0, RoundingMode.DOWN);

        InvestOrder order = InvestOrder.create(accountNo, req.assetNo(), req.side(), req.qty(), price, "system");
        orderRepo.save(order);

        try {
            if ("BUY".equals(req.side())) {
                executeBuy(account, order, req.qty(), price, execAmt);
            } else {
                executeSell(account, order, req.qty(), price, execAmt);
            }
        } catch (ApiException e) {
            order.reject(e.getMessage());
            throw e;
        }

        return InvestOrderRes.from(order);
    }

    @Transactional(readOnly = true)
    public List<InvestOrderRes> getOrders(Long accountNo) {
        return orderRepo.findByAccountNoOrderByOrderDtDesc(accountNo).stream()
                .map(InvestOrderRes::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InvestHoldingRes> getHoldings(Long accountNo) {
        return holdingRepo.findByIdAccountNo(accountNo).stream()
                .map(InvestHoldingRes::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InvestLedgerRes> getLedger(Long accountNo) {
        return ledgerRepo.findByAccountNoOrderByRegDtDesc(accountNo).stream()
                .map(InvestLedgerRes::from).toList();
    }

    // ── private ───────────────────────────────────────────────────────────────

    private void executeBuy(InvestAccount account, InvestOrder order,
                             BigDecimal qty, BigDecimal price, BigDecimal execAmt) {
        BigDecimal fee = execAmt.multiply(FEE_RATE).setScale(0, RoundingMode.UP);
        BigDecimal totalCost = execAmt.add(fee);

        if (account.getDepositAmt().compareTo(totalCost) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "예수금 부족: 필요 " + totalCost + ", 보유 " + account.getDepositAmt());
        }

        account.deductDeposit(totalCost);
        order.fill(qty);

        InvestExecution exec = InvestExecution.of(order.getOrderNo(), qty, price, execAmt, fee, BigDecimal.ZERO);
        execRepo.save(exec);

        InvestHoldingId holdingId = new InvestHoldingId(account.getAccountNo(), order.getAssetNo());
        InvestHolding holding = holdingRepo.findById(holdingId).orElse(null);
        if (holding == null) {
            holdingRepo.save(InvestHolding.create(account.getAccountNo(), order.getAssetNo(), qty, price, execAmt));
        } else {
            holding.addBuy(qty, execAmt);
        }

        ledgerRepo.save(AccountLedger.of(
                account.getAccountNo(), "BUY", totalCost.negate(), account.getDepositAmt(),
                "ORDER", order.getOrderNo(),
                String.format("매수 assetNo=%d qty=%s @%s fee=%s", order.getAssetNo(), qty, price, fee)
        ));
    }

    private void executeSell(InvestAccount account, InvestOrder order,
                              BigDecimal qty, BigDecimal price, BigDecimal execAmt) {
        InvestHoldingId holdingId = new InvestHoldingId(account.getAccountNo(), order.getAssetNo());
        InvestHolding holding = holdingRepo.findById(holdingId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "보유 종목이 없습니다."));

        if (holding.getSellableQty().compareTo(qty) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "매도가능 수량 부족: 필요 " + qty + ", 보유 " + holding.getSellableQty());
        }

        BigDecimal fee = execAmt.multiply(FEE_RATE).setScale(0, RoundingMode.UP);
        BigDecimal tax = execAmt.multiply(TAX_RATE).setScale(0, RoundingMode.UP);
        BigDecimal proceeds = execAmt.subtract(fee).subtract(tax);

        account.addDeposit(proceeds);
        order.fill(qty);
        holding.reduceSell(qty);

        InvestExecution exec = InvestExecution.of(order.getOrderNo(), qty, price, execAmt, fee, tax);
        execRepo.save(exec);

        ledgerRepo.save(AccountLedger.of(
                account.getAccountNo(), "SELL", proceeds, account.getDepositAmt(),
                "ORDER", order.getOrderNo(),
                String.format("매도 assetNo=%d qty=%s @%s fee=%s tax=%s", order.getAssetNo(), qty, price, fee, tax)
        ));
    }

    private BigDecimal currentPrice(Long assetNo) {
        return assetRepo.findRealtimePrice(assetNo)
                .or(() -> assetRepo.findLatestDailyPrice(assetNo))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "현재 시세 정보가 없습니다."));
    }

    private InvestAccount findAccount(Long accountNo) {
        return accountRepo.findById(accountNo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다."));
    }

    private void validateOrderReq(InvestOrderReq req) {
        if (req.assetNo() == null || req.qty() == null || req.side() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "assetNo, side, qty는 필수입니다.");
        }
        if (!List.of("BUY", "SELL").contains(req.side())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "side는 BUY 또는 SELL 이어야 합니다.");
        }
        if (req.qty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "수량은 0보다 커야 합니다.");
        }
    }
}
