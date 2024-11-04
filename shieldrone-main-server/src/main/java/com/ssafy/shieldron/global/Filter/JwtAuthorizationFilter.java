package com.ssafy.shieldron.global.Filter;

import com.ssafy.shieldron.global.UserContext;
import com.ssafy.shieldron.global.util.JwtUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@WebFilter("/*")
@RequiredArgsConstructor
public class JwtAuthorizationFilter implements Filter {

    private final JwtUtil jwtUtil;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            // 엑세스 토큰이 없음
            return;
        }

        String token = authorization.substring(7);
        if (!jwtUtil.validateToken(token)) {
            // 서명 검증
        }
        if (jwtUtil.isExpired(token)) {
            // 기간 만료됐으므로 응답 코드를 보낸 후 리프레시토큰 개발급하도록 한다.
        }

        String phoneNumber = jwtUtil.getPhoneNumber(token);
        if (phoneNumber != null) {
            UserContext.setPhoneNumber(phoneNumber);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
