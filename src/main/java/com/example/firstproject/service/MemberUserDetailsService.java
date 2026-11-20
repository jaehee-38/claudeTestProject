package com.example.firstproject.service;

import com.example.firstproject.entity.Member;
import com.example.firstproject.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("아이디 또는 비밀번호가 올바르지 않습니다."));

        boolean enabled   = !"WITHDRAWN".equals(member.getMemberStatCd());
        boolean nonLocked = !"LOCKED".equals(member.getMemberStatCd());

        return new User(
                member.getLoginId(),
                member.getPassword(),
                enabled,
                true,
                true,
                nonLocked,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );
    }
}
