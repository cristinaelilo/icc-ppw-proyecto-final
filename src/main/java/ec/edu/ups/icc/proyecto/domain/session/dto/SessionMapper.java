package ec.edu.ups.icc.proyecto.domain.session.dto;

import ec.edu.ups.icc.proyecto.domain.session.model.Session;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper {
    public SessionResponse toResponse(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getEvent().getId(),
                session.getTitle(),
                session.getDescription(),
                session.getStartAt(),
                session.getEndAt(),
                session.getLocation(),
                session.getVirtualUrl()
        );
    }
}
