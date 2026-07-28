package ec.edu.ups.icc.proyecto.domain.event.dto;

import ec.edu.ups.icc.proyecto.domain.event.model.Event;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getModality(),
                event.getLocation(),
                event.getVirtualUrl(),
                event.getCapacity(),
                event.getAvailableCapacity(),
                event.getRegistrationStartAt(),
                event.getRegistrationEndAt(),
                event.getStartAt(),
                event.getEndAt(),
                event.getStatus(),
                event.getCategory().getId(),
                event.getCategory().getName(),
                event.getOrganizer().getId(),
                event.getOrganizer().getFullName(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
