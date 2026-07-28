package ec.edu.ups.icc.proyecto.domain.session.controllers;

import ec.edu.ups.icc.proyecto.domain.session.dto.SessionRequest;
import ec.edu.ups.icc.proyecto.domain.session.dto.SessionResponse;
import ec.edu.ups.icc.proyecto.domain.session.services.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public List<SessionResponse> findByEvent(@PathVariable Long eventId) {
        return sessionService.findByEvent(eventId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<SessionResponse> create(@PathVariable Long eventId, @Valid @RequestBody SessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.create(eventId, request));
    }

    @PutMapping("/{sessionId}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public SessionResponse update(@PathVariable Long eventId, @PathVariable Long sessionId,
                                   @Valid @RequestBody SessionRequest request) {
        return sessionService.update(eventId, sessionId, request);
    }

    @DeleteMapping("/{sessionId}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long eventId, @PathVariable Long sessionId) {
        sessionService.delete(eventId, sessionId);
        return ResponseEntity.noContent().build();
    }
}
