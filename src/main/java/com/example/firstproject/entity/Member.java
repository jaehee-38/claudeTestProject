package com.example.firstproject.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_MEMBER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MEMBER_NO")
    private Long memberNo;

    @Column(name = "LOGIN_ID", length = 50, nullable = false)
    private String loginId;

    @Column(name = "PASSWORD", length = 100, nullable = false)
    private String password;

    @Column(name = "MEMBER_NM", length = 50)
    private String memberNm;

    @Column(name = "MEMBER_STAT_CD", length = 20)
    private String memberStatCd;

    @Column(name = "LOGIN_FAIL_CNT")
    private int loginFailCnt;

    @Column(name = "LAST_LOGIN_DT")
    private LocalDateTime lastLoginDt;

    @Column(name = "REG_DT")
    private LocalDateTime regDt;

    public void recordLogin() {
        this.loginFailCnt = 0;
        this.lastLoginDt = LocalDateTime.now();
    }

    public void increaseFailCount() {
        this.loginFailCnt++;
        if (this.loginFailCnt >= 5) {
            this.memberStatCd = "LOCKED";
        }
    }
}
