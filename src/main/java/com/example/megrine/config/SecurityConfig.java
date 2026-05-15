package com.example.megrine.config;

import com.example.megrine.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired private CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Ressources publiques
                .requestMatchers("/css/**","/js/**","/images/**","/icons/**",
                                 "/manifest.json","/sw.js").permitAll()

                // ADMIN seulement
                .requestMatchers("/database/**","/users/**",
                                 "/admin/**",
                                 "/training/new","/training/delete/**","/training/edit/**",
                                 "/events/participants/*/complete",
                                 "/volunteers/hours/**").hasRole("ADMIN")

                // MEMBER + ADMIN (espace membre)
                .requestMatchers("/member/**",
                                 "/training/**",
                                 "/events/participate/**",
                                 "/events/cancel/**").hasAnyRole("MEMBER","ADMIN","USER")

                // Tout le reste : connecte
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );
        return http.build();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder ab = http.getSharedObject(AuthenticationManagerBuilder.class);
        ab.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return ab.build();
    }
}
