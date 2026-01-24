package com.example.unbox_product.common.config;

import com.example.unbox_common.config.CorsProperties;
import com.example.unbox_common.security.jwt.JwtFilter;
import com.example.unbox_common.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorize 사용을 위해
@RequiredArgsConstructor
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // 권한 설정: 구체적인 URL 권한은 Controller의 @PreAuthorize에서 처리 권장
        // 혹은 여기서 전역 정책 설정. 일단 인증된 사용자 접근을 기본으로 함.
        // product 서비스 특성상 조회(GET)은 열어둘 수도 있으나, 일단 unbox_be 정책을 따름.
        http.authorizeHttpRequests(auth -> auth
            // Swagger 등 허용
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll()
            .requestMatchers("/actuator/**").permitAll()
            // Internal API (Feign) 허용
            .requestMatchers("/internal/**").permitAll()
            // 관리자 API 권한 설정 (Authority 명시)
            .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_MASTER", "ROLE_MANAGER")
            // 그 외 모든 요청 인증 필요
            .anyRequest().authenticated()
        );

        // JWT Filter 추가
        http.addFilterBefore(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration obj = new CorsConfiguration();
        obj.setAllowedOrigins(corsProperties.allowedOrigins());
        obj.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        obj.setAllowedHeaders(List.of("*"));
        obj.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", obj);
        return source;
    }
}
