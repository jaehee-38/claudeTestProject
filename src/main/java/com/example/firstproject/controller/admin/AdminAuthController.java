package com.example.firstproject.controller.admin;

import com.example.firstproject.config.AdminAuthFilter;
import com.example.firstproject.config.AdminProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 관리자 로그인 / 로그아웃 (로그인 화면은 static/admin/login.html).
 */
@Controller
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminProperties props;

    @GetMapping("/admin/login")
    public String loginPage() {
        return "forward:/admin/login.html";
    }

    // 로그인 폼 제출: POST /admin/login (username, password)
    @PostMapping("/admin/login")
    public String login(@RequestParam(defaultValue = "") String username,
                        @RequestParam(defaultValue = "") String password,
                        HttpServletRequest request) {
        // 둘 다 비교해서 어느 쪽이 틀렸는지 시간 차로 드러나지 않게 합니다
        boolean userOk = same(username.trim(), props.username());
        boolean passOk = same(password, props.password());
        if (!(props.configured() && userOk & passOk)) {
            return "redirect:/admin/login?error";
        }
        HttpSession session = request.getSession(true);
        request.changeSessionId(); // 로그인 전 세션 ID 를 그대로 쓰지 않도록 새로 발급
        session.setAttribute(AdminAuthFilter.SESSION_KEY, Boolean.TRUE);
        return "redirect:/admin/";
    }

    // 로그아웃: POST /admin/logout
    @PostMapping("/admin/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return "redirect:/admin/login?logout";
    }

    private static boolean same(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),
                (b == null ? "" : b).getBytes(StandardCharsets.UTF_8));
    }
}
