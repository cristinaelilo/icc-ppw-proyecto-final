package ec.edu.ups.icc.proyecto.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/** Extiende el User de Spring Security para exponer el id interno del usuario. */
public class UserPrincipal extends User {

    private final Long id;

    public UserPrincipal(Long id, String email, String passwordHash, Collection<? extends GrantedAuthority> authorities) {
        super(email, passwordHash == null ? "" : passwordHash, authorities);
        this.id = id;
    }

    public Long getId() { return id; }

    public static UserPrincipal from(Long id, String email, String passwordHash, Collection<String> roles) {
        var authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        return new UserPrincipal(id, email, passwordHash, authorities);
    }
}
