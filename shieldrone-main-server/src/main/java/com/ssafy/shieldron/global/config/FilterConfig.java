package com.ssafy.shieldron.global.config;

import com.ssafy.shieldron.global.Filter.JwtAuthorizationFilter;
import com.ssafy.shieldron.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class FilterConfig {

    private final JwtUtil jwtUtil;

    @Bean
    public FilterRegistrationBean<JwtAuthorizationFilter> jwtFilter() {
        FilterRegistrationBean<JwtAuthorizationFilter> registry = new FilterRegistrationBean<>();
        registry.setFilter(new JwtAuthorizationFilter(jwtUtil));
        registry.addUrlPatterns("/*");
        registry.setOrder(1);
        return registry;
    }
}
