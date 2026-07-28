package ec.edu.ups.icc.proyecto.security.jwt;

import ec.edu.ups.icc.proyecto.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/** Extrae y valida el access token JWT del header Authorization: Bearer ... */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtTokenProvider.parseClaims(token);
                if ("access".equals(claims.get("type"))) {
                    // IMPORTANTE: con jjwt-gson (necesario para evitar el conflicto con Jackson 3
                    // de Spring Boot 4), los numeros genericos del JWT se deserializan como Double,
                    // nunca como Long/Integer. JJWT NO convierte automaticamente Double->Long
                    // (solo String/Date/Long/Integer/Short/Byte), asi que se pide como Number
                    // (Double SI es un Number, no requiere conversion) y se trunca con longValue().
                    Long userId = claims.get("uid", Number.class).longValue();
                    String email = claims.getSubject();
                    List<String> roles = claims.get("roles", List.class);
                    Collection<String> roleNames = roles == null ? List.of() : roles;

                    UserPrincipal principal = UserPrincipal.from(userId, email, null, roleNames);
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ex) {
                // Token invalido/expirado: se deja sin autenticar; Spring Security respondera 401/403.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}