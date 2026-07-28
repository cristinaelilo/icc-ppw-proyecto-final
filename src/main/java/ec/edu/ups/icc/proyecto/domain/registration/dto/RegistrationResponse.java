package ec.edu.ups.icc.proyecto.domain.registration.dto;

import ec.edu.ups.icc.proyecto.domain.registration.model.RegistrationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegistrationResponse(
        Long id,
        UUID registrationCode,
        Long eventId,
        String eventTitle,
        Long participantId,
        String participantName,
        RegistrationStatus status,
        OffsetDateTime registeredAt,
        OffsetDateTime statusUpdatedAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime cancelledAt
) {}
