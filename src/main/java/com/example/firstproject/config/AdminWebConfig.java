package com.example.firstproject.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 사용자 / 관리자 화면 분리.
 *   /          → static/index.html        (사용자)
 *   /admin/**  → static/admin/index.html  (관리자, 비밀번호 필요)
 */
@Configuration
public class AdminWebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/admin").setViewName("forward:/admin/index.html");
        registry.addViewController("/admin/").setViewName("forward:/admin/index.html");
    }

    @Bean
    public FilterRegistrationBean<AdminAuthFilter> adminAuthFilter(AdminProperties props) {
        FilterRegistrationBean<AdminAuthFilter> bean = new FilterRegistrationBean<>(new AdminAuthFilter(props));
        bean.addUrlPatterns("/admin", "/admin/*");
        bean.setOrder(1);
        return bean;
    }
}
