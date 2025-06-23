package com.add.venture.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/**",
                                "/chat/**", 
                                "/notificaciones/*/marcar-leida",
                                "/notificaciones/marcar-todas-leidas",
                                "/notificaciones/responder-solicitud",
                                "/grupos/*/unirse")) // Ignorar CSRF para API y AJAX calls

                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas (sin autenticación)
                        .requestMatchers(
                                "/auth/**",
                                "/api/**",
                                "/support/**",
                                "/css/**",
                                "/js/**",
                                "/usuarios/**",
                                "/images/**",
                                "/uploads/**", // Para servir imágenes del chat
                                "/")
                        .permitAll()
                        // Rutas de grupos - públicas para ver, autenticadas para acciones
                        .requestMatchers(
                                "/grupos", 
                                "/grupos/buscar")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET, 
                                "/grupos/*") // Ver detalles de grupo (GET público)
                        .permitAll()
                        // Rutas que requieren autenticación
                        .requestMatchers(
                                "/grupos/*/unirse",
                                "/grupos/*/abandonar", 
                                "/grupos/*/expulsar",
                                "/grupos/*/historial-chat",
                                "/grupos/crear",
                                "/grupos/editar/**",
                                "/chat/**",
                                "/notificaciones/**",
                                "/calificaciones/**",
                                "/perfil/**",
                                "/user/**")
                        .authenticated()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/auth/login") // tu login web personalizado
                        .loginProcessingUrl("/auth/login") // POST del formulario
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/auth/login?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        // .logoutSuccessUrl("/auth/login?logout=true")
                        .logoutSuccessUrl("/")
                        .permitAll());

        return http.build();
    }
}
