package ec.edu.ups.icc.proyecto.config;

import ec.edu.ups.icc.proyecto.common.ratelimit.RateLimitFilter;
import ec.edu.ups.icc.proyecto.security.jwt.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * NOTA: el login de la API (email + password de negocio) se valida
 * manualmente en AuthServiceImpl usando PasswordEncoder + BCrypt, NO a traves
 * de un AuthenticationManager de Spring Security. El AuthenticationManager /
 * HttpBasic configurado aqui se usa EXCLUSIVAMENTE para proteger
 * /swagger-ui/** y /v3/api-docs/** con credenciales de evaluacion
 * independientes (SWAGGER_USER / SWAGGER_PASSWORD).
 *
 * Actuator: solo /actuator/health esta expuesto (management.endpoints.web.exposure.include=health
 * en application.yml); cualquier otro path de /actuator/** se deniega explicitamente aqui
 * como capa adicional de defensa.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.swagger.username}")
    private String swaggerUsername;

    @Value("${app.swagger.password}")
    private String swaggerPassword;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, RateLimitFilter rateLimitFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService swaggerUserDetailsService() {
        return new InMemoryUserDetailsManager(
                User.withUsername(swaggerUsername)
                        .password(swaggerPassword)
                        .roles("SWAGGER")
                        .build()
        );
    }

    @Bean
    public AuthenticationManager swaggerAuthenticationManager(UserDetailsService swaggerUserDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(swaggerUserDetailsService);
        provider.setPasswordEncoder(NoOpPasswordEncoder.getInstance());
        return new ProviderManager(provider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager swaggerAuthenticationManager) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationManager(swaggerAuthenticationManager)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout")
                        .permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/actuator/**").denyAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/events/**", "/api/categories/**")
                        .permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").authenticated()
                    .anyRequest().authenticated()
            )
            .httpBasic(basic -> {}) // solo se activa realmente para /swagger-ui/** y /v3/api-docs/**
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
