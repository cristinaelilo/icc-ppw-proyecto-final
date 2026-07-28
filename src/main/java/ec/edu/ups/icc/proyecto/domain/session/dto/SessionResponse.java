package ec.edu.ups.icc.proyecto.domain.session.dto;

import java.time.OffsetDateTime;

public record SessionResponse(
        Long id,
        Long eventId,
        String title,
        String description,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String location,
        String virtualUrl
) {}
