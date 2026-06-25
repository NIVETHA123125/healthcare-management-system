package com.healthcare.system.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private SessionInterceptor sessionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/", 
                    "/about", 
                    "/contact", 
                    "/register", 
                    "/register/save",
                    "/forgot-password", 
                    "/reset-password",
                    "/login", 
                    "/logout", 
                    "/css/**", 
                    "/js/**", 
                    "/favicon.ico", 
                    "/error"
                );
    }
}
