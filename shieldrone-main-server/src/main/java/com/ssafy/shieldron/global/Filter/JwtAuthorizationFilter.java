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


@RequiredArgsConstructor
public class JwtAuthorizationFilter implements Filter {

    private final JwtUtil jwtUtil;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\": \"AUTHORIZATION_HEADER_MISSING\", \"message\": \"Authorization header is missing or invalid\"}");
            response.getWriter().flush();
            return;
        }

        String token = authorization.substring(7);
        if (!jwtUtil.validateToken(token)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\": \"INVALID_TOKEN\", \"message\": \"Invalid token\"}");
            response.getWriter().flush();
            return;
        }

        if (jwtUtil.isExpired(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\": \"TOKEN_EXPIRED\", \"message\": \"Token is expired\"}");
            response.getWriter().flush();
            return;
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
