package com.alan.project.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 全局跨域配置
 *
 * 使用 CorsFilter（Servlet 级，最高优先级）而不是 addCorsMappings：
 * 后者挂在处理器链上，请求在进入 Controller 前失败（如文件超限的 MultipartException）
 * 时响应会走 /error 兜底通道，不带 CORS 头，浏览器直接拦截，前端只能看到 Network Error。
 *
 * @author alan
 */
@Configuration
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许发送 Cookie
        config.setAllowCredentials(true);
        // 放行所有域名（必须用 patterns，否则 * 会和 allowCredentials 冲突）
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有接口生效
        source.registerCorsConfiguration("/**", config);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        // 最高优先级，先于 multipart 解析等一切环节执行
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
