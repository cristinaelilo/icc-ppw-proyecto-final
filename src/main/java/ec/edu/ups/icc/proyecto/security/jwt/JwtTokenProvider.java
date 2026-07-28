package ec.edu.ups.icc.proyecto.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Genera y valida access tokens (JWT firmados, corta duracion) y refresh
 * tokens (JWT firmados, ademas persistidos en la tabla refresh_tokens por
 * su hash para poder revocarlos/rotarlos del lado del servidor).
 */
@Component
public class JwtTokenProvider {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String email, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("uid", userId)
                .claim("roles", roles)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(properties.getAccessExpirationMs())))
                .signWith(key)
                .compact();
    }

    /** El jti (tokenId) debe coincidir con refresh_tokens.token_id para poder rastrear la rotacion. */
    public String generateRefreshToken(Long userId, String email, UUID tokenId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(tokenId.toString())
                .subject(email)
                .claim("uid", userId)
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(properties.getRefreshExpirationMs())))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getRefreshExpirationMs() {
        return properties.getRefreshExpirationMs();
    }

    /** Los refresh tokens se guardan hasheados (SHA-256), nunca en texto plano. */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo hashear el token", e);
        }
    }
}
