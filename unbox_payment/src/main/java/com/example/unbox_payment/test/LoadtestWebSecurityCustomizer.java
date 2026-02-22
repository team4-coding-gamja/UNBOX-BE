package com.example.unbox_payment.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@Configuration
public class LoadtestWebSecurityCustomizer {

    @Bean
    public WebSecurityCustomizer loadtestWebIgnoringCustomizer() {
        // loadtest 전용 엔드포인트는 JWT 생성 없이 k6에서 바로 호출할 수 있도록 인증 필터를 우회한다.
        return web -> web.ignoring().requestMatchers(
                "/mock/**",
                "/payment/mock/**",
                "/test/api/payment/**",
                "/payment/test/api/payment/**");
    }
}
