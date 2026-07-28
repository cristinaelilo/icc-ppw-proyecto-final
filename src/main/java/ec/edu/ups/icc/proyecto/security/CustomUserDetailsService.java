package ec.edu.ups.icc.proyecto.security;

import ec.edu.ups.icc.proyecto.domain.user.model.Role;
import ec.edu.ups.icc.proyecto.domain.user.model.User;
import ec.edu.ups.icc.proyecto.domain.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * NOTA: este servicio no se usa a traves del AuthenticationManager de Spring
 * Security (el login se valida manualmente en AuthServiceImpl con
 * PasswordEncoder). Se deja disponible para el futuro (ej. HTTP Basic en
 * Swagger, o pruebas con @WithUserDetails).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales invalidas"));

        var roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return UserPrincipal.from(user.getId(), user.getEmail(), user.getPasswordHash(), roles);
    }
}
