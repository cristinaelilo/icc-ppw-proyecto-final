package ec.edu.ups.icc.proyecto.domain.registration.dto;

import ec.edu.ups.icc.proyecto.domain.registration.model.Registration;
import org.springframework.stereotype.Component;

@Component
public class RegistrationMapper {
    public RegistrationResponse toResponse(Registration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getRegistrationCode(),
                registration.getEvent().getId(),
                registration.getEvent().getTitle(),
                registration.getParticipant().getId(),
                registration.getParticipant().getFullName(),
                registration.getStatus(),
                registration.getRegisteredAt(),
                registration.getStatusUpdatedAt(),
                registration.getConfirmedAt(),
                registration.getCancelledAt()
        );
    }
}
