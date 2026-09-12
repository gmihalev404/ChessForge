package com.example.chessforge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(
                                        "/",
                                        "/login",
                                        "/register"
                                )
                                .permitAll()

                                .requestMatchers(
                                        "/ws",
                                        "/ws/**",
                                        "/games/**",
                                        "/friends/**",
                                        "/challenges/**",
                                        "/tournaments/**"
                                )
                                .authenticated()

                                .anyRequest()
                                .permitAll()
                )
                .formLogin(form ->
                        form
                                .loginPage(
                                        "/login"
                                )
                                .defaultSuccessUrl(
                                        "/",
                                        true
                                )
                                .permitAll()
                )
                .logout(
                        Customizer.withDefaults()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }
}