package com.example.firstproject.controller.admin;

import com.example.firstproject.common.ApiException;
import com.example.firstproject.dto.AdminInvestAccountRes;
import com.example.firstproject.dto.AdminMemberRes;
import com.example.firstproject.entity.InvestAccount;
import com.example.firstproject.entity.Member;
import com.example.firstproject.repository.InvestAccountRepository;
import com.example.firstproject.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberRepository memberRepo;
    private final InvestAccountRepository accountRepo;

    // 회원 목록 (전체 or 상태 필터)
    @GetMapping("/members")
    @Transactional(readOnly = true)
    public List<AdminMemberRes> members(@RequestParam(required = false) String stat) {
        List<Member> list = (stat != null && !stat.isBlank())
                ? memberRepo.findByMemberStatCd(stat)
                : memberRepo.findAll();
        return list.stream()
                .sorted((a, b) -> Long.compare(a.getMemberNo(), b.getMemberNo()))
                .map(AdminMemberRes::from)
                .toList();
    }

    // 회원 상태 변경 (NORMAL / LOCKED)
    @PatchMapping("/members/{memberNo}/status")
    @Transactional
    public AdminMemberRes changeMemberStatus(@PathVariable Long memberNo,
                                              @RequestBody Map<String, String> body) {
        String statCd = body.get("statCd");
        if (!List.of("NORMAL", "LOCKED", "DORMANT").contains(statCd)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 상태 코드입니다: " + statCd);
        }
        Member member = memberRepo.findById(memberNo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
        member.updateStatus(statCd);
        return AdminMemberRes.from(member);
    }

    // 모의투자 계좌 목록 (전체 or 특정 회원)
    @GetMapping("/invest-accounts")
    @Transactional(readOnly = true)
    public List<AdminInvestAccountRes> investAccounts(@RequestParam(required = false) Long memberNo) {
        List<InvestAccount> accounts = (memberNo != null)
                ? accountRepo.findByMemberNo(memberNo)
                : accountRepo.findAll();

        // memberNo → loginId 맵 (N+1 방지)
        Map<Long, String> loginIdMap = memberRepo.findAllById(
                accounts.stream().map(InvestAccount::getMemberNo).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Member::getMemberNo, Member::getLoginId));

        return accounts.stream()
                .sorted((a, b) -> Long.compare(a.getAccountNo(), b.getAccountNo()))
                .map(a -> AdminInvestAccountRes.of(a, loginIdMap.getOrDefault(a.getMemberNo(), "-")))
                .toList();
    }
}
