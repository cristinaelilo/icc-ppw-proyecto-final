package ec.edu.ups.icc.proyecto.config;

import ec.edu.ups.icc.proyecto.security.jwt.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JwtAuthenticationFilter es un bean @Component (para poder inyectarlo en
 * SecurityConfig y anadirlo explicitamente con addFilterBefore en el orden
 * correcto). Sin esto, Spring Boot lo auto-registraria TAMBIEN como filtro
 * de servlet global (por implementar Filter), ejecutandolo dos veces por
 * request. Este bean desactiva ese auto-registro global.
 */
@Configuration
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtFilterAutoRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
