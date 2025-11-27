package com.add.venture.config;

import com.add.venture.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Los orígenes permitidos se configuran en application.properties
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Habilitar CORS con la configuración personalizada
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/**",
                                "/chat/**", 
                                "/notificaciones/*/marcar-leida",
                                "/notificaciones/marcar-todas-leidas",
                                "/notificaciones/responder-solicitud",
                                "/grupos/*/unirse"))
                
                // Configurar sesiones como STATELESS para JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Permitir solicitudes OPTIONS para CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        
                        // Rutas públicas (sin autenticación)
                        .requestMatchers(
                                "/api/auth/**", // Endpoints de autenticación JWT
                                "/api/home", // Endpoint público de home (lista grupos)
                                "/api/grupos", // Endpoint público de búsqueda de grupos
                                "/api/grupos/*/permisos", // Endpoint de permisos (necesario para botones)
                                "/api/support/**", // Endpoints de soporte
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**",
                                "/ws/**", // Endpoint WebSocket (handshake - auth en interceptor)
                                "/",
                                "/auth/**") // Permitir acceso a páginas de login/registro
                        .permitAll()
                        // GET de detalle de grupo - público para ver
                        .requestMatchers(HttpMethod.GET, "/api/grupos/*")
                        .permitAll()
                        // Rutas API protegidas que requieren JWT (incluyendo chat)
                        .requestMatchers("/api/**")
                        .authenticated()
                        // Rutas que requieren autenticación (form-based o JWT)
                        .requestMatchers(
                                "/grupos/*/unirse",
                                "/grupos/*/abandonar", 
                                "/grupos/*/expulsar",
                                "/grupos/*/galeria-fotos",
                                "/grupos/*/descargar-fotos",
                                "/grupos/crear",
                                "/grupos/editar/**",
                                "/chat/**",
                                "/notificaciones/**",
                                "/calificaciones/**",
                                "/perfil/**",
                                "/user/**"
                                )
                        .authenticated()
                        .anyRequest().authenticated())
                
                // Agregar filtro JWT antes del filtro de autenticación estándar
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                
                // Configurar manejador de excepciones para rutas API
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            String requestUri = request.getRequestURI();
                            // Si es una petición API o WebSocket, devolver 401 JSON sin redirección
                            if (requestUri.startsWith("/api/") || requestUri.startsWith("/ws/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json;charset=UTF-8");
                                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                                response.getWriter().write("{\"error\": \"No autenticado\", \"message\": \"Token invalido o expirado\"}");
                                response.getWriter().flush();
                            } else {
                                // Para rutas web, redirigir al login
                                response.sendRedirect("/auth/login");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String requestUri = request.getRequestURI();
                            if (requestUri.startsWith("/api/") || requestUri.startsWith("/ws/")) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json;charset=UTF-8");
                                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                                response.getWriter().write("{\"error\": \"Acceso denegado\", \"message\": \"" + accessDeniedException.getMessage() + "\"}");
                                response.getWriter().flush();
                            } else {
                                response.sendRedirect("/auth/login?error=forbidden");
                            }
                        })
                )
                
                // Form login solo para rutas NO-API
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/auth/login?error=true")
                        .permitAll())
                
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/")
                        .permitAll());

        return http.build();
    }
}
