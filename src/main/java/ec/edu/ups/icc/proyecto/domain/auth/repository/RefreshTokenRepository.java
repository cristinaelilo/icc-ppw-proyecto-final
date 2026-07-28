package ec.edu.ups.icc.proyecto.domain.auth.repository;

import ec.edu.ups.icc.proyecto.domain.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);
}
