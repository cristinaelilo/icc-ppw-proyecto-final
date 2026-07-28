package ec.edu.ups.icc.proyecto.domain.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record SessionRequest(
        @NotBlank(message = "El titulo de la sesion es obligatorio") String title,
        String description,
        @NotNull(message = "La hora de inicio es obligatoria") OffsetDateTime startAt,
        @NotNull(message = "La hora de fin es obligatoria") OffsetDateTime endAt,
        String location,
        String virtualUrl
) {}
