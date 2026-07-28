package ec.edu.ups.icc.proyecto.domain.auth.services;

import ec.edu.ups.icc.proyecto.common.exception.AccountBlockedException;
import ec.edu.ups.icc.proyecto.common.exception.DuplicateResourceException;
import ec.edu.ups.icc.proyecto.common.ratelimit.RateLimitProperties;
import ec.edu.ups.icc.proyecto.common.ratelimit.RateLimitService;
import ec.edu.ups.icc.proyecto.domain.auth.dto.LoginRequest;
import ec.edu.ups.icc.proyecto.domain.auth.dto.RegisterRequest;
import ec.edu.ups.icc.proyecto.domain.auth.repository.RefreshTokenRepository;
import ec.edu.ups.icc.proyecto.domain.user.dto.UserMapper;
import ec.edu.ups.icc.proyecto.domain.user.dto.UserResponse;
import ec.edu.ups.icc.proyecto.domain.user.model.Role;
import ec.edu.ups.icc.proyecto.domain.user.model.User;
import ec.edu.ups.icc.proyecto.domain.user.model.UserStatus;
import ec.edu.ups.icc.proyecto.domain.user.repository.RoleRepository;
import ec.edu.ups.icc.proyecto.domain.user.repository.UserRepository;
import ec.edu.ups.icc.proyecto.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas de las reglas de negocio mas sensibles de autenticacion:
 * - No se puede registrar un correo duplicado.
 * - Credenciales invalidas (correo inexistente o password incorrecta) dan el mismo error generico.
 * - Una cuenta BLOCKED no puede iniciar sesion aunque la contrasena sea correcta.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RateLimitService rateLimitService;
    @Mock private UserMapper userMapper;

    private RateLimitProperties rateLimitProperties;
    private AuthServiceImpl authService;

    private static final String CLIENT_IP = "127.0.0.1";

    @BeforeEach
    void setUp() {
        rateLimitProperties = new RateLimitProperties();
        authService = new AuthServiceImpl(userRepository, roleRepository, refreshTokenRepository,
                passwordEncoder, jwtTokenProvider, rateLimitService, rateLimitProperties, userMapper);

        var rule = rateLimitProperties.getRegister();
        lenient().when(rateLimitService.increment(anyString(), eq(rule.getLimit()), eq(rule.getWindowSeconds())))
                .thenReturn(new RateLimitService.RateLimitResult(true, 1, rule.getLimit(), rule.getWindowSeconds()));

        var loginRule = rateLimitProperties.getLogin();
        lenient().when(rateLimitService.increment(startsWith("rate:login:"), eq(loginRule.getLimit()), eq(loginRule.getWindowSeconds())))
                .thenReturn(new RateLimitService.RateLimitResult(true, 1, loginRule.getLimit(), loginRule.getWindowSeconds()));

        lenient().when(rateLimitService.isBlocked(anyString(), anyString())).thenReturn(false);
    }

    @Test
    void register_deberiaFallarSiElCorreoYaExiste() {
        when(userRepository.existsByEmailIgnoreCase("existente@ups.edu.ec")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("Juan", "Perez", "existente@ups.edu.ec", "Password123");

        assertThatThrownBy(() -> authService.register(request, CLIENT_IP))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_deberiaAsignarRolParticipantPorDefecto() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        Role participant = new Role();
        when(roleRepository.findByName(Role.PARTICIPANT)).thenReturn(Optional.of(participant));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(1L, "Ana", "Lopez", "ana@ups.edu.ec", UserStatus.ACTIVE, java.util.Set.of("PARTICIPANT"), null));

        RegisterRequest request = new RegisterRequest("Ana", "Lopez", "ana@ups.edu.ec", "Password123");
        authService.register(request, CLIENT_IP);

        verify(userRepository).save(argThat(u -> u.getRoles().contains(participant)));
    }

    @Test
    void login_deberiaFallarConCredencialesInvalidas_correoInexistente() {
        when(userRepository.findByEmailIgnoreCase("noexiste@ups.edu.ec")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("noexiste@ups.edu.ec", "cualquiera");

        assertThatThrownBy(() -> authService.login(request, CLIENT_IP))
                .isInstanceOf(BadCredentialsException.class);

        verify(rateLimitService).registerFailedAttempt(eq(CLIENT_IP), eq("noexiste@ups.edu.ec"), anyInt(), anyLong());
    }

    @Test
    void login_deberiaFallarConCredencialesInvalidas_passwordIncorrecta() {
        User user = new User();
        user.setEmail("carlos@ups.edu.ec");
        user.setPasswordHash("hash-correcto");
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmailIgnoreCase("carlos@ups.edu.ec")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("mal", "hash-correcto")).thenReturn(false);

        LoginRequest request = new LoginRequest("carlos@ups.edu.ec", "mal");

        assertThatThrownBy(() -> authService.login(request, CLIENT_IP))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_deberiaFallarSiLaCuentaEstaBloqueada() {
        User user = new User();
        user.setEmail("bloqueado@ups.edu.ec");
        user.setPasswordHash("hash-correcto");
        user.setStatus(UserStatus.BLOCKED);

        when(userRepository.findByEmailIgnoreCase("bloqueado@ups.edu.ec")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correcta", "hash-correcto")).thenReturn(true);

        LoginRequest request = new LoginRequest("bloqueado@ups.edu.ec", "correcta");

        assertThatThrownBy(() -> authService.login(request, CLIENT_IP))
                .isInstanceOf(AccountBlockedException.class);
    }
}
