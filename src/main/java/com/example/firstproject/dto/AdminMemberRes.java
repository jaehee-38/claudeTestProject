package com.example.firstproject.dto;

import com.example.firstproject.entity.Member;

import java.time.LocalDateTime;

public record AdminMemberRes(
        Long memberNo,
        String loginId,
        String memberNm,
        String memberStatCd,
        int loginFailCnt,
        LocalDateTime lastLoginDt,
        LocalDateTime regDt
) {
    public static AdminMemberRes from(Member m) {
        return new AdminMemberRes(
                m.getMemberNo(), m.getLoginId(), m.getMemberNm(),
                m.getMemberStatCd(), m.getLoginFailCnt(),
                m.getLastLoginDt(), m.getRegDt()
        );
    }
}
