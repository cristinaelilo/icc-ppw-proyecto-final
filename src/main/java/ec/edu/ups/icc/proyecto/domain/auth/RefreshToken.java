package ec.edu.ups.icc.proyecto.domain.auth;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador incluido como "jti" dentro del propio JWT de refresh. */
    @Column(name = "token_id", nullable = false, unique = true)
    private UUID tokenId = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Nunca se guarda el token en texto plano, solo su hash (SHA-256). */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by_ip", length = 45)
    private String createdByIp;

    /** Encadena el token anterior con el nuevo cuando se rota (refresh). */
    @Column(name = "replaced_by_token_id")
    private UUID replacedByTokenId;

    public Long getId() { return id; }
    public UUID getTokenId() { return tokenId; }
    public void setTokenId(UUID tokenId) { this.tokenId = tokenId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public OffsetDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(OffsetDateTime revokedAt) { this.revokedAt = revokedAt; }
    public boolean isRevoked() { return revokedAt != null; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getCreatedByIp() { return createdByIp; }
    public void setCreatedByIp(String createdByIp) { this.createdByIp = createdByIp; }
    public UUID getReplacedByTokenId() { return replacedByTokenId; }
    public void setReplacedByTokenId(UUID replacedByTokenId) { this.replacedByTokenId = replacedByTokenId; }
}
