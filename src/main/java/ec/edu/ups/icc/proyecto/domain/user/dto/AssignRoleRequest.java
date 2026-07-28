package ec.edu.ups.icc.proyecto.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleRequest(
        @NotBlank(message = "El rol es obligatorio") String role
) {}
