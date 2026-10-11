package com.hotel.config;

import com.hotel.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void json(HttpServletResponse res, int status, String body) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(body);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .userDetailsService(userDetailsService)

            // The frontend is plain HTML + fetch(), so there is no Thymeleaf CSRF token.
            // Fine for a learning project; see notes for production.
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                // static pages are just shells; real protection is on the API
                .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/rooms", "/api/rooms/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )

            // POST /api/auth/login  (form fields: email, password) -> JSON response
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler((req, res, authn) ->
                        json(res, 200, "{\"message\":\"Login successful\"}"))
                .failureHandler((req, res, ex) ->
                        json(res, 401, "{\"message\":\"Invalid email or password\"}"))
                .permitAll()
            )

            // POST /api/auth/logout
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, authn) ->
                        json(res, 200, "{\"message\":\"Logged out\"}"))
                .permitAll()
            )

            // API calls without a session get 401 JSON instead of a redirect to a login page
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                        json(res, 401, "{\"message\":\"Please sign in first\"}"))
                .accessDeniedHandler((req, res, e) ->
                        json(res, 403, "{\"message\":\"You do not have permission to do that\"}"))
            );

        return http.build();
    }
}
