package ec.edu.ups.icc.proyecto.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.ups.icc.proyecto.common.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Aplica limites generales de solicitudes:
 * - Endpoints publicos: por IP (60/min por defecto)
 * - Endpoints autenticados: por usuario (120/min por defecto)
 *
 * Los limites especificos de login/registro se aplican dentro de
 * AuthServiceImpl (necesitan la clave IP+correo, no solo IP o usuario).
 * El de reportes se aplicara en el controlador de reportes cuando se cree.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(RateLimitService rateLimitService, RateLimitProperties properties) {
        this.rateLimitService = rateLimitService;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Actuator health no se limita para no bloquear monitoreo/evaluacion.
        return path.startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());

        String key;
        long limit;
        long windowSeconds;

        if (authenticated) {
            key = "rate:auth:" + auth.getName();
            limit = properties.getAuthenticated().getLimit();
            windowSeconds = properties.getAuthenticated().getWindowSeconds();
        } else {
            String ip = resolveClientIp(request);
            key = "rate:public:" + ip;
            limit = properties.getPublic().getLimit();
            windowSeconds = properties.getPublic().getWindowSeconds();
        }

        RateLimitService.RateLimitResult result = rateLimitService.increment(key, limit, windowSeconds);

        if (!result.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse body = new ErrorResponse(
                    OffsetDateTime.now(),
                    429,
                    "RATE_LIMIT_EXCEEDED",
                    "Ha excedido el limite de solicitudes permitidas. Intente nuevamente mas tarde.",
                    request.getRequestURI(),
                    null
            );
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
