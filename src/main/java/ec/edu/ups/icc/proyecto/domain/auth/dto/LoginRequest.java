package ec.edu.ups.icc.proyecto.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo invalido") String email,
        @NotBlank(message = "La contrasena es obligatoria") String password
) {}
