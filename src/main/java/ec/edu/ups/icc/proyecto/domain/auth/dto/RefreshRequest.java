package ec.edu.ups.icc.proyecto.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "El refresh token es obligatorio") String refreshToken
) {}
