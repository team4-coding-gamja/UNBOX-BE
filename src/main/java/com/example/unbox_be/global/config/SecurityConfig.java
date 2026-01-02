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
import com.example.unbox_be.global.security.exception.CustomAuthenticationEntryPoint;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final ObjectMapper objectMapper;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // ✅ PasswordEncoder는 인터페이스 타입으로 Bean 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ✅ CORS는 명시적으로 허용 Origin을 지정 (쿠키 사용 시 필수)
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

                // ✅ 로그인 안 됐을 때 JSON 메시지 내려줌 (인증 필요한 요청에서만 동작)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/api-docs/**", "/swagger-resources/**"
                        ).permitAll()

                        // ✅ 테스트용 부트스트랩(운영 시 삭제)
                        .requestMatchers(HttpMethod.POST, "/api/test/bootstrap/master").permitAll()

                        // 유저 인증 관련
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/reissue"
                        ).permitAll()

                        // ✅ 관리자 로그인/재발급은 열어둠
                        .requestMatchers(HttpMethod.POST,
                                "/api/admin/auth/login",
                                "/api/admin/auth/reissue"
                        ).permitAll()

                        // ✅ 관리자 생성(회원가입)은 MASTER만 가능
                        .requestMatchers(HttpMethod.POST,
                                "/api/admin/auth/signup"
                        ).hasRole("MASTER")   // ROLE_MASTER 필요

                        // ✅ 관리자 staff 관련(목록/상세/수정/삭제 전부) MASTER만
                        .requestMatchers(
                                "/api/admin/staff"
                        ).hasRole("MASTER")

                        // ✅ 관리자 staff 관련(목록/상세/수정/삭제 전부) MASTER만
                        .requestMatchers(
                                "/api/admin/staff"
                        ).hasRole("MASTER")

                        // ✅ 관리자 staff 관련(목록/상세/수정/삭제 전부) MASTER만
                        .requestMatchers(
                                "/api/admin/staff",
                                "/api/admin/staff/**"
                        ).hasRole("MASTER")

                        // 로그아웃은 인증 필요(선택)
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/logout",
                                "/api/admin/auth/logout"
                        ).authenticated()

                        // ✅ 관리자 API 전체 보호 (3개 롤 허용)
                        .requestMatchers("/api/admin/**")
                        .hasAnyRole("MASTER", "MANAGER", "INSPECTOR")

                        .anyRequest().authenticated()
                );

        // ✅ 필터 구성 (실무 권장 순서)
        // 1) LogoutFilter (토큰/쿠키 제거, Redis 삭제 등) - 인증 필터보다 앞
        http.addFilterBefore(
                new CustomLogoutFilter(jwtUtil, refreshTokenRedisRepository),
                UsernamePasswordAuthenticationFilter.class
        );
        // 1) 유저 로그인 필터
        LoginFilter userLoginFilter = new LoginFilter(
                authenticationManager(authenticationConfiguration),
                jwtUtil,
                refreshTokenRedisRepository,
                objectMapper
        );
        userLoginFilter.setFilterProcessesUrl("/api/auth/login");
        http.addFilterAt(userLoginFilter, UsernamePasswordAuthenticationFilter.class);

        // 2) 관리자 로그인 필터
        LoginFilter adminLoginFilter = new LoginFilter(
                authenticationManager(authenticationConfiguration),
                jwtUtil,
                refreshTokenRedisRepository,
                objectMapper
        );
        adminLoginFilter.setFilterProcessesUrl("/api/admin/auth/login");
        http.addFilterAt(adminLoginFilter, UsernamePasswordAuthenticationFilter.class);

        // 3) JwtFilter - 나머지 요청 Authorization 검증
        http.addFilterAfter(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
