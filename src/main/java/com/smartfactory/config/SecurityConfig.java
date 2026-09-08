package com.smartfactory.config;

import com.smartfactory.security.JwtAuthenticationFilter;
import com.smartfactory.security.JwtTokenBlacklistService;
import com.smartfactory.security.JwtTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 密码加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtProperties jwtProperties(
            @Value("${smart-factory.jwt.secret}") String secret,
            @Value("${smart-factory.jwt.expiration-seconds:7200}")
            long expirationSeconds) {
        return new JwtProperties(secret, expirationSeconds);
    }

    @Bean
    public JwtTokenService jwtTokenService(
            JwtProperties jwtProperties) {
        return new JwtTokenService(jwtProperties);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            org.springframework.security.core.userdetails.UserDetailsService
                    userDetailsService,
            JwtTokenBlacklistService jwtTokenBlacklistService) {
        return new JwtAuthenticationFilter(
                jwtTokenService,
                userDetailsService,
                jwtTokenBlacklistService
        );
    }

    /**
     * Spring Security 安全规则
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(login -> login.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                (request, response, authException) ->
                                        writeSecurityError(
                                                response,
                                                HttpServletResponse
                                                        .SC_UNAUTHORIZED,
                                                40100,
                                                "未登录或登录已过期"
                                        )
                        )
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) ->
                                        writeSecurityError(
                                                response,
                                                HttpServletResponse
                                                        .SC_FORBIDDEN,
                                                40300,
                                                "无权访问"
                                        )
                        )
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    private void writeSecurityError(
            HttpServletResponse response,
            int status,
            int code,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"code\":" + code
                        + ",\"message\":\"" + message
                        + "\",\"data\":null}"
        );
    }
}
