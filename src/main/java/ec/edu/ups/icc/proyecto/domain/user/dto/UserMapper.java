package ec.edu.ups.icc.proyecto.domain.user.dto;

import ec.edu.ups.icc.proyecto.domain.user.model.Role;
import ec.edu.ups.icc.proyecto.domain.user.model.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getStatus(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                user.getCreatedAt()
        );
    }
}
