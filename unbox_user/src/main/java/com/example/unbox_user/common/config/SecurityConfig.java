package com.example.unbox_user.common.config;

import com.example.unbox_user.common.security.login.CustomLogoutFilter;
import com.example.unbox_user.common.security.login.LoginFilter;
import com.example.unbox_user.common.security.token.RefreshTokenRedisRepository;
import com.example.unbox_common.config.CorsConfigUtil;
import com.example.unbox_common.config.CorsProperties;
import com.example.unbox_common.error.exception.CustomAuthenticationEntryPoint;
import com.example.unbox_common.security.jwt.JwtFilter;
import com.example.unbox_common.security.jwt.JwtUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final ObjectMapper objectMapper;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CorsProperties corsProperties;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // common 모듈의 유틸 사용
        source.registerCorsConfiguration("/**", CorsConfigUtil.getCorsConfig(corsProperties));

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                );

        http.authorizeHttpRequests(auth -> auth
                // ... 기존 URL 설정 그대로 유지 ...
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                        "/", "/swagger-ui/**", "/swagger-ui.html",
                        "/v3/api-docs/**", "/api-docs/**", "/swagger-resources/**",
                        // ALB prefix 경로 허용
                        "/user/swagger-ui/**", "/user/swagger-ui.html",
                        "/user/v3/api-docs/**", "/user/api-docs/**",
                        "/actuator/**"
                ).permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/test/bootstrap/master").permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/signup",
                        "/api/auth/login",
                        "/api/auth/reissue"
                ).permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/api/admin/auth/login",
                        "/api/admin/auth/reissue"
                ).permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/api/admin/auth/signup"
                ).hasRole("MASTER")
                .requestMatchers(
                        "/api/admin/staff/me",
                        "/api/admin/staff/me/**"
                ).hasAnyRole("MASTER", "MANAGER", "INSPECTOR")
                .requestMatchers(
                        "/api/admin/staff",
                        "/api/admin/staff/**"
                ).hasRole("MASTER")
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/logout",
                        "/api/admin/auth/logout"
                ).authenticated()
                .requestMatchers("/api/admin/**")
                .hasAnyRole("MASTER", "MANAGER", "INSPECTOR")
                .anyRequest().authenticated()
        );

        // 필터 설정 (로컬 필터 + 공통 필터 조립)

        // 1. 로그아웃 필터 (로컬)
        http.addFilterBefore(
                new CustomLogoutFilter(jwtUtil, refreshTokenRedisRepository),
                UsernamePasswordAuthenticationFilter.class
        );

        // 2. 유저 로그인 필터 (로컬)
        LoginFilter userLoginFilter = new LoginFilter(
                authenticationManager(authenticationConfiguration),
                jwtUtil,
                refreshTokenRedisRepository,
                objectMapper
        );
        userLoginFilter.setFilterProcessesUrl("/api/auth/login");
        http.addFilterAt(userLoginFilter, UsernamePasswordAuthenticationFilter.class);

        // 3. 관리자 로그인 필터 (로컬)
        LoginFilter adminLoginFilter = new LoginFilter(
                authenticationManager(authenticationConfiguration),
                jwtUtil,
                refreshTokenRedisRepository,
                objectMapper
        );
        adminLoginFilter.setFilterProcessesUrl("/api/admin/auth/login");
        http.addFilterAt(adminLoginFilter, UsernamePasswordAuthenticationFilter.class);

        // 4. JWT 검증 필터 (공통 모듈 사용)
        http.addFilterAfter(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}