package ec.edu.ups.icc.proyecto.domain.event.dto;

import ec.edu.ups.icc.proyecto.domain.event.model.EventModality;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record EventRequest(
        @NotBlank(message = "El titulo es obligatorio") String title,
        @NotBlank(message = "La descripcion es obligatoria") String description,
        @NotNull(message = "La modalidad es obligatoria") EventModality modality,
        String location,
        String virtualUrl,
        @NotNull(message = "El cupo es obligatorio") @Min(value = 1, message = "El cupo debe ser mayor a 0") Integer capacity,
        @NotNull(message = "La fecha de inicio de inscripciones es obligatoria") OffsetDateTime registrationStartAt,
        @NotNull(message = "La fecha de fin de inscripciones es obligatoria") OffsetDateTime registrationEndAt,
        @NotNull(message = "La fecha de inicio del evento es obligatoria") OffsetDateTime startAt,
        @NotNull(message = "La fecha de fin del evento es obligatoria") OffsetDateTime endAt,
        @NotNull(message = "La categoria es obligatoria") Long categoryId
) {}
