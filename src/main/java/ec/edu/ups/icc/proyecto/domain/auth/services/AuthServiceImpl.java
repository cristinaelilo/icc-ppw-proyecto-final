package ec.edu.ups.icc.proyecto.domain.auth.services;

import ec.edu.ups.icc.proyecto.common.exception.AccountBlockedException;
import ec.edu.ups.icc.proyecto.common.exception.DuplicateResourceException;
import ec.edu.ups.icc.proyecto.common.exception.InvalidTokenException;
import ec.edu.ups.icc.proyecto.common.exception.ResourceNotFoundException;
import ec.edu.ups.icc.proyecto.common.exception.TooManyRequestsException;
import ec.edu.ups.icc.proyecto.common.ratelimit.RateLimitProperties;
import ec.edu.ups.icc.proyecto.common.ratelimit.RateLimitService;
import ec.edu.ups.icc.proyecto.domain.auth.dto.LoginRequest;
import ec.edu.ups.icc.proyecto.domain.auth.dto.RefreshRequest;
import ec.edu.ups.icc.proyecto.domain.auth.dto.RegisterRequest;
import ec.edu.ups.icc.proyecto.domain.auth.dto.TokenResponse;
import ec.edu.ups.icc.proyecto.domain.auth.model.RefreshToken;
import ec.edu.ups.icc.proyecto.domain.auth.repository.RefreshTokenRepository;
import ec.edu.ups.icc.proyecto.domain.user.dto.UserMapper;
import ec.edu.ups.icc.proyecto.domain.user.dto.UserResponse;
import ec.edu.ups.icc.proyecto.domain.user.model.Role;
import ec.edu.ups.icc.proyecto.domain.user.model.User;
import ec.edu.ups.icc.proyecto.domain.user.model.UserStatus;
import ec.edu.ups.icc.proyecto.domain.user.repository.RoleRepository;
import ec.edu.ups.icc.proyecto.domain.user.repository.UserRepository;
import ec.edu.ups.icc.proyecto.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RateLimitService rateLimitService;
    private final RateLimitProperties rateLimitProperties;
    private final UserMapper userMapper;

    public AuthServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                            RefreshTokenRepository refreshTokenRepository,
                            PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider,
                            RateLimitService rateLimitService, RateLimitProperties rateLimitProperties,
                            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.rateLimitService = rateLimitService;
        this.rateLimitProperties = rateLimitProperties;
        this.userMapper = userMapper;
    }

    @Override
    public UserResponse register(RegisterRequest request, String clientIp) {
        var rule = rateLimitProperties.getRegister();
        var result = rateLimitService.increment("rate:register:" + clientIp, rule.getLimit(), rule.getWindowSeconds());
        if (!result.allowed()) {
            throw new TooManyRequestsException("Demasiados registros desde esta direccion. Intente mas tarde.",
                    result.retryAfterSeconds());
        }

        String normalizedEmail = request.email().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("No fue posible completar el registro con los datos proporcionados");
        }

        Role participantRole = roleRepository.findByName(Role.PARTICIPANT)
                .orElseThrow(() -> new ResourceNotFoundException("El rol PARTICIPANT no esta configurado en la base de datos"));

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        Set<Role> roles = new HashSet<>();
        roles.add(participantRole);
        user.setRoles(roles);

        userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    public TokenResponse login(LoginRequest request, String clientIp) {
        String email = request.email().toLowerCase();

        var loginRule = rateLimitProperties.getLogin();
        var rateResult = rateLimitService.increment("rate:login:" + clientIp + ":" + email,
                loginRule.getLimit(), loginRule.getWindowSeconds());
        if (!rateResult.allowed()) {
            throw new TooManyRequestsException("Demasiados intentos de inicio de sesion. Intente mas tarde.",
                    rateResult.retryAfterSeconds());
        }

        if (rateLimitService.isBlocked(clientIp, email)) {
            throw new TooManyRequestsException(
                    "La direccion IP y/o correo estan temporalmente bloqueados por multiples intentos fallidos.",
                    rateLimitProperties.getBlock().getBlockDurationSeconds());
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            rateLimitService.registerFailedAttempt(clientIp, email,
                    rateLimitProperties.getBlock().getMaxFailedAttempts(),
                    rateLimitProperties.getBlock().getBlockDurationSeconds());
            throw new BadCredentialsException("Credenciales invalidas");
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountBlockedException("Su cuenta se encuentra bloqueada. Contacte a un administrador.");
        }

        rateLimitService.clearFailedAttempts(clientIp, email);
        return issueTokenPair(user, clientIp).response();
    }

    @Override
    public TokenResponse refresh(RefreshRequest request, String clientIp) {
        Claims claims;
        try {
            claims = jwtTokenProvider.parseClaims(request.refreshToken());
        } catch (Exception e) {
            throw new InvalidTokenException("Refresh token invalido o expirado");
        }
        if (!"refresh".equals(claims.get("type"))) {
            throw new InvalidTokenException("El token proporcionado no es un refresh token");
        }

        String tokenHash = jwtTokenProvider.hashToken(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token invalido, expirado o revocado"));

        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidTokenException("Refresh token expirado");
        }

        Long userId = Long.valueOf(String.valueOf(claims.get("uid")));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("Usuario no encontrado para este token"));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountBlockedException("Su cuenta se encuentra bloqueada. Contacte a un administrador.");
        }

        TokenPair newTokens = issueTokenPair(user, clientIp);

        stored.setRevokedAt(OffsetDateTime.now());
        stored.setReplacedByTokenId(newTokens.tokenId());
        refreshTokenRepository.save(stored);

        return newTokens.response();
    }

    @Override
    public void logout(RefreshRequest request) {
        String tokenHash = jwtTokenProvider.hashToken(request.refreshToken());
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash).ifPresent(rt -> {
            rt.setRevokedAt(OffsetDateTime.now());
            refreshTokenRepository.save(rt);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("Usuario no encontrado"));
        return userMapper.toResponse(user);
    }

    private TokenPair issueTokenPair(User user, String clientIp) {
        List<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
        UUID tokenId = UUID.randomUUID();

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), roleNames);
        String refreshTokenJwt = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail(), tokenId);

        RefreshToken entity = new RefreshToken();
        entity.setTokenId(tokenId);
        entity.setUserId(user.getId());
        entity.setTokenHash(jwtTokenProvider.hashToken(refreshTokenJwt));
        entity.setExpiresAt(OffsetDateTime.now().plusNanos(jwtTokenProvider.getRefreshExpirationMs() * 1_000_000));
        entity.setCreatedByIp(clientIp);
        refreshTokenRepository.save(entity);

        TokenResponse response = new TokenResponse(accessToken, refreshTokenJwt, "Bearer",
                jwtTokenProvider.getRefreshExpirationMs() / 1000);
        return new TokenPair(response, tokenId);
    }

    private record TokenPair(TokenResponse response, UUID tokenId) {}
}
