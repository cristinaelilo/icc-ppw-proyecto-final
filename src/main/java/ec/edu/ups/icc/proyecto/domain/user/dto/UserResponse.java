package ec.edu.ups.icc.proyecto.domain.user.dto;

import ec.edu.ups.icc.proyecto.domain.user.model.UserStatus;

import java.time.OffsetDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        UserStatus status,
        Set<String> roles,
        OffsetDateTime createdAt
) {}
