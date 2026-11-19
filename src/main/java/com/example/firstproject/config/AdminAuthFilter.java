package com.example.firstproject.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * /admin/** (화면 + API) 를 로그인 세션으로 막습니다.
 *  - 로그인 안 된 상태로 화면에 오면 → /admin/login 으로 보냄
 *  - 로그인 안 된 상태로 /admin/api/** 를 부르면 → 401 JSON
 * ADMIN_PASSWORD 가 비어 있으면 관리자 페이지 전체를 잠가 둡니다 (실수로 열린 채 배포되지 않도록).
 */
public class AdminAuthFilter extends OncePerRequestFilter {

    /** 세션에 이 값이 true 로 들어 있으면 관리자 로그인 상태 */
    public static final String SESSION_KEY = "ADMIN_LOGIN";

    /** 로그인 없이 열리는 경로 (로그인 화면 자체) */
    private static final Set<String> PUBLIC_PATHS = Set.of("/admin/login", "/admin/login.html");

    private final AdminProperties props;

    public AdminAuthFilter(AdminProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (!props.configured()) {
            sendJson(res, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "관리자 비밀번호가 설정되지 않았습니다. .env 에 ADMIN_PASSWORD 값을 넣고 서버를 다시 실행하세요.");
            return;
        }
        if (PUBLIC_PATHS.contains(path) || isLoggedIn(req)) {
            chain.doFilter(req, res);
            return;
        }
        if (path.startsWith("/admin/api/")) {
            sendJson(res, HttpServletResponse.SC_UNAUTHORIZED, "관리자 로그인이 필요합니다.");
        } else {
            res.sendRedirect(req.getContextPath() + "/admin/login");
        }
    }

    private static boolean isLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_KEY));
    }

    private static void sendJson(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"message\":\"" + message.replace("\"", "\\\"") + "\"}");
    }
}
