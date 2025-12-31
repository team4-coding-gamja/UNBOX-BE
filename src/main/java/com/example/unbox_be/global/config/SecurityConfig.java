package com.example.unbox_be.global.config;

import com.example.unbox_be.global.security.jwt.JwtFilter;
import com.example.unbox_be.global.security.jwt.JwtUtil;
import com.example.unbox_be.global.security.login.CustomLogoutFilter;
import com.example.unbox_be.global.security.login.LoginFilter;
import com.example.unbox_be.global.security.token.RefreshTokenRedisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final ObjectMapper objectMapper;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // ✅ 실무: PasswordEncoder는 인터페이스 타입으로 Bean 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ✅ 실무: CORS는 명시적으로 허용 Origin을 지정 (쿠키 사용 시 필수)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return (HttpServletRequest request) -> {
            CorsConfiguration config = new CorsConfiguration();

            // 예시: 프론트가 로컬/배포 둘 다 있을 때
            config.setAllowedOrigins(List.of("*"
                    // "https://your-frontend-domain.com"
            ));

            config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
            config.setAllowedHeaders(List.of("Authorization","Content-Type"));
            config.setExposedHeaders(List.of("Authorization"));
            config.setAllowCredentials(true); // ✅ refresh 쿠키 쓰면 true 필수
            config.setMaxAge(3600L);

            return config;
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // ✅ REST/JWT 기본
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ✅ 권한 설정 (실무: 경로는 최대한 “패턴으로” 관리)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/api/auth/login",
                                "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/api-docs/**", "/swagger-resources/**"
                        ).permitAll()

                        // 회원가입/로그인/재발급은 인증 없이
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/reissue"
                        ).permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()

                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                );

        // ✅ 필터 구성 (실무 권장 순서)
        // 1) LogoutFilter (토큰/쿠키 제거, Redis 삭제 등) - 인증 필터보다 앞
        http.addFilterBefore(
                new CustomLogoutFilter(jwtUtil, refreshTokenRedisRepository),
                UsernamePasswordAuthenticationFilter.class
        );

        // 2) LoginFilter - UsernamePasswordAuthenticationFilter 자리 “대체”
        LoginFilter loginFilter = new LoginFilter(
                authenticationManager(authenticationConfiguration),
                jwtUtil,
                refreshTokenRedisRepository,
                objectMapper
        );

        loginFilter.setFilterProcessesUrl("/api/auth/login"); // ✅ 실무: 명시적으로 지정
        http.addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

        // 3) JwtFilter - 나머지 요청 Authorization 검증
        http.addFilterAfter(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
