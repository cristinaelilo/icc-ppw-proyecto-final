package ec.edu.ups.icc.proyecto.domain.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres") String name,
        @Size(max = 255, message = "La descripcion no puede superar 255 caracteres") String description
) {}
