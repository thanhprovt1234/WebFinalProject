package com.example.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.DispatcherType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http, CustomUserDetailsService users,
                        com.example.demo.service.GuestCartService guests) throws Exception {
                var loginSuccess=new org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler();
                loginSuccess.setDefaultTargetUrl("/");
                http
                                .authorizeHttpRequests(auth -> auth
                                                .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR)
                                                .permitAll()
                                                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/seller", "/seller/**").hasAnyRole("VENDOR", "ADMIN")
                                                .requestMatchers("/manager", "/manager/**").hasAnyRole("MANAGER", "ADMIN")
                                                .requestMatchers("/shipper", "/shipper/**").hasAnyRole("SHIPPER", "ADMIN")
                                                .requestMatchers(
                                                                "/",
                                                                "/api/cart", "/api/cart/**",
                                                                "/collections/ao-thun-co-tron",
                                                                "/collections",
                                                                "/products/*",
                                                                "/login",
                                                                "/register",
                                                                "/forgot-password",
                                                                "/verify-otp",
                                                                "/reset-password",
                                                                "/css/**",
                                                                "/js/**",
                                                                "/images/**")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .addFilterBefore(new CurrentRolesFilter(users), org.springframework.security.web.access.intercept.AuthorizationFilter.class)
                                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, exception) -> {
                                        response.setStatus(403);
                                        if (request.getServletPath().startsWith("/api/")) {
                                                response.setContentType("application/json");
                                                response.getWriter().write("{\"error\":\"FORBIDDEN\"}");
                                        } else {
                                                request.getRequestDispatcher("/WEB-INF/views/workspace/forbidden.jsp").forward(request, response);
                                        }
                                }).authenticationEntryPoint((request, response, exception) -> {
                                        if (request.getServletPath().startsWith("/api/")) {
                                                response.sendError(401);
                                        } else {
                                                new org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint("/login")
                                                        .commence(request, response, exception);
                                        }
                                }))
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .successHandler((request,response,authentication)-> {
                                                        var session=request.getSession(false);
                                                        if(session!=null) guests.merge(session,authentication.getName());
                                                        loginSuccess.onAuthenticationSuccess(request,response,authentication);
                                                })
                                                .failureUrl("/login?error")
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout")
                                                .permitAll());

                return http.build();
        }

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
