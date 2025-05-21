package com.vehicle.salesmanagement.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/user").authenticated()
                        //.requestMatchers("/api/orders/**").authenticated()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().permitAll()
                )
                .httpBasic(httpBasic -> httpBasic.realmName("Vehicle Sales Management")); // Use HTTP Basic Auth

        return http.build();
    }

    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        return username -> {
            if ("admin".equals(username)) {
                return org.springframework.security.core.userdetails.User
                        .withUsername("admin")
                        .password("{noop}admin123")
                        .roles("ADMIN")
                        .build();
            } else if ("manager".equals(username)) {
                return org.springframework.security.core.userdetails.User
                        .withUsername("manager")
                        .password("{noop}manager123")
                        .roles("MANAGER")
                        .build();
            } else if ("user".equals(username)) {
                return org.springframework.security.core.userdetails.User
                        .withUsername("user")
                        .password("{noop}user123")
                        .roles("USER")
                        .build();
            }
            throw new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found");
        };
    }
}