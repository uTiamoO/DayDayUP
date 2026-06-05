package com.yuan.daydayup.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 视图控制器配置。
 *
 * <p>注册无业务逻辑的纯视图映射。当前仅登录页：{@code GET /login} → {@code templates/login.html}。</p>
 *
 * <p>背景：{@link AuthSecurityConfig} 用 {@code formLogin().loginPage("/login")} 指定了自定义
 * 登录页，此时 Spring Security 不再生成内置默认登录页，必须由应用自身渲染 {@code GET /login}，
 * 否则授权码流程跳转登录页时会 404。登录表单的提交（{@code POST /login}）由 Spring Security 的
 * {@code UsernamePasswordAuthenticationFilter} 处理，因此登录页是纯视图，用 view controller
 * 即可，无需 {@code @Controller}。</p>
 */
@Configuration(proxyBeanMethods = false)
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
    }
}
