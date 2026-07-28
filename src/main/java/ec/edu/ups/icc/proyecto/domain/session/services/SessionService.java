package ec.edu.ups.icc.proyecto.domain.session.services;

import ec.edu.ups.icc.proyecto.domain.session.dto.SessionRequest;
import ec.edu.ups.icc.proyecto.domain.session.dto.SessionResponse;

import java.util.List;

public interface SessionService {
    List<SessionResponse> findByEvent(Long eventId);
    SessionResponse create(Long eventId, SessionRequest request);
    SessionResponse update(Long eventId, Long sessionId, SessionRequest request);
    void delete(Long eventId, Long sessionId);
}
