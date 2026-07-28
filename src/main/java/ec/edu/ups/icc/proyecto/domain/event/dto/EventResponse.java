package ec.edu.ups.icc.proyecto.domain.event.dto;

import ec.edu.ups.icc.proyecto.domain.event.model.EventModality;
import ec.edu.ups.icc.proyecto.domain.event.model.EventStatus;

import java.time.OffsetDateTime;

public record EventResponse(
        Long id,
        String title,
        String description,
        EventModality modality,
        String location,
        String virtualUrl,
        Integer capacity,
        Integer availableCapacity,
        OffsetDateTime registrationStartAt,
        OffsetDateTime registrationEndAt,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        EventStatus status,
        Long categoryId,
        String categoryName,
        Long organizerId,
        String organizerName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
